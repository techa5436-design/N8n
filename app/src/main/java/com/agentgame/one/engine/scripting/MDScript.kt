package com.agentgame.one.engine.scripting

import com.agentgame.one.engine.core.Node
import com.agentgame.one.engine.core.Vector2
import kotlin.math.PI

/**
 * A compact GDScript-like interpreter that powers node scripts. It supports variables, control
 * flow, functions, arithmetic/comparison/logic, arrays, property access, method calls on nodes,
 * and the special `self` (the attached node). Attach with `node.attachScript(source)`.
 *
 * Example:
 * ```
 * var speed = 120
 * func _ready():
 *     print("hello ", self.name)
 * func _process(delta):
 *     if Input.isActionPressed("ui_right"):
 *         self.position.x += speed * delta
 * ```
 */
class MDScript private constructor(
    private val node: Node,
    private val source: String,
    private val program: List<Stmt>,
    private val globals: Env,
) : NodeScript {

    companion object {
        fun compile(node: Node, source: String): MDScript {
            val tokens = Lexer(braceify(source)).tokenize()
            val stmts = Parser(tokens).parse()
            return MDScript(node, source, stmts, Env().apply { set("self", node) })
        }

        /**
         * Translates GDScript-style `:` + indentation blocks into brace blocks so the parser can
         * consume them. Also handles `else` / `elif`. Plain `{ }` code passes through unchanged.
         */
        fun braceify(src: String): String {
            val sb = StringBuilder()
            val stack = ArrayDeque<Int>()
            for (raw in src.split("\n")) {
                val trimmed = raw.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("#") || trimmed.startsWith("//")) continue
                val indent = (raw.takeWhile { it == ' ' || it == '\t' }).length
                val isElse = trimmed == "else"
                val isElif = trimmed.startsWith("elif") || trimmed.startsWith("else if")

                if (isElse || isElif) {
                    // close the just-finished if block, then re-open as else
                    if (stack.isNotEmpty()) { sb.append("}\n"); stack.removeLast() }
                    val t = if (isElif) trimmed.replaceFirst(Regex("^(else\\s+if|elif)\\b"), "else if") else "else"
                    sb.append(if (t.endsWith(":")) t.dropLast(1).trimEnd() + " {" else t).append("\n")
                    stack.addLast(indent)
                    continue
                }

                // close any block whose indentation is >= the current line (dedent / sibling)
                while (stack.isNotEmpty() && indent <= stack.last()) {
                    sb.append("}\n")
                    stack.removeLast()
                }
                var t = trimmed
                if (t.endsWith(":") && !t.contains("{")) {
                    t = t.dropLast(1).trimEnd() + " {"
                    stack.addLast(indent)
                }
                sb.append(t).append("\n")
            }
            while (stack.isNotEmpty()) { sb.append("}\n"); stack.removeLast() }
            return sb.toString()
        }
    }

    override fun sourceCode(): String = source

    private var mainRun = false

    override fun _onReady() {
        if (!mainRun) {
            mainRun = true
            runScriptCallbacks(program)
        }
        invokeHandler("_ready", emptyArray())
    }

    override fun _onEnterTree() {
        // `_ready` is invoked once the node is inside the tree (see _onReady).
    }

    override fun _onExitTree() {
        invokeHandler("_exit_tree", emptyArray())
    }

    override fun onProcess(delta: Float) {
        globals.set("delta", delta)
        invokeHandler("_process", arrayOf(delta))
    }

    override fun onPhysicsProcess(delta: Float) {
        globals.set("delta", delta)
        invokeHandler("_physics_process", arrayOf(delta))
    }

    override fun onInput(event: Any) {
        invokeHandler("_input", arrayOf(event))
    }

    override fun invokeSignalHandler(signalName: String, source: Node, args: Array<Any?>) {
        val handler = "on_" + signalName
        invokeHandler(handler, args)
    }

    private fun invokeHandler(name: String, args: Array<Any?>) {
        val fn = globals.get(name) as? MDScriptFunction ?: return
        val env = Env(globals)
        bindParams(fn, args, env)
        Interpreter(node, env).executeBlock(fn.body)
    }

    /** Runs top-level statements once (script "main"). Runs in the shared globals scope so that
     *  `func _process(...)` and friends are registered where the lifecycle hooks look for them. */
    private fun runScriptCallbacks(stmts: List<Stmt>) {
        globals.set("self", node)
        Interpreter(node, globals).executeBlock(stmts)
    }

    /** Internal: exposes the parsed top-level program (used by the debug runner). */
    fun programForDebug(): List<Stmt> = program

    override fun toString(): String = "MDScript[$source]"

    // ------------------------------------------------------------------
    //  AST
    // ------------------------------------------------------------------
    sealed class Stmt
    class VarStmt(val name: String, val init: Expr?) : Stmt()
    class AssignStmt(val target: Expr, val value: Expr) : Stmt()
    class ExprStmt(val expr: Expr) : Stmt()
    class IfStmt(val cond: Expr, val then: List<Stmt>, val otherwise: List<Stmt>) : Stmt()
    class WhileStmt(val cond: Expr, val body: List<Stmt>) : Stmt()
    class ForStmt(val varName: String, val iter: Expr, val body: List<Stmt>) : Stmt()
    class FuncStmt(val name: String, val params: List<String>, val body: List<Stmt>) : Stmt()
    class ReturnStmt(val value: Expr?) : Stmt()

    sealed class Expr
    class Literal(val value: Any?) : Expr()
    class VarExpr(val name: String) : Expr()
    class SelfExpr : Expr()
    class BinExpr(val op: String, val left: Expr, val right: Expr) : Expr()
    class CompoundAssignExpr(val target: Expr, val op: String, val value: Expr) : Expr()
    class UnaryExpr(val op: String, val operand: Expr) : Expr()
    class CallExpr(val callee: Expr, val args: List<Expr>) : Expr()
    class MemberExpr(val obj: Expr, val name: String) : Expr()
    class IndexExpr(val obj: Expr, val index: Expr) : Expr()
    class ArrayLit(val items: List<Expr>) : Expr()

    // ------------------------------------------------------------------
    //  Runtime values
    // ------------------------------------------------------------------
    class MDScriptFunction(val params: List<String>, val body: List<Stmt>, val closure: Env)

    class RuntimeReturn(val value: Any?) : RuntimeException()

    // ------------------------------------------------------------------
    //  Lexer
    // ------------------------------------------------------------------
    class Lexer(private val src: String) {
        private var pos = 0
        private val tokens = mutableListOf<Token>()

        class Token(val type: String, val text: String, val value: Any?)

        fun tokenize(): List<Token> {
            while (pos < src.length) {
                val c = src[pos]
                when {
                    c == '\n' || c == ';' -> { tokens.add(Token("SEMI", "\n", null)); pos++ }
                    c.isWhitespace() -> pos++
                    c == '#' || (c == '/' && peek(1) == '/') -> skipComment()
                    c == '"' || c == '\'' -> readString(c)
                    c.isDigit() -> readNumber()
                    c.isLetter() || c == '_' -> readIdentifier()
                    else -> readOperator()
                }
            }
            tokens.add(Token("EOF", "", null))
            return tokens
        }

        private fun peek(off: Int): Char = if (pos + off < src.length) src[pos + off] else '\u0000'

        private fun skipComment() {
            while (pos < src.length && src[pos] != '\n') pos++
        }

        private fun readString(quote: Char) {
            val start = pos
            pos++
            val sb = StringBuilder()
            while (pos < src.length) {
                val ch = src[pos]
                if (ch == '\\' && pos + 1 < src.length) {
                    pos++
                    when (src[pos]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        '\\' -> sb.append('\\')
                        '"' -> sb.append('"')
                        '\'' -> sb.append('\'')
                        else -> sb.append(src[pos])
                    }
                    pos++
                } else if (ch == quote) {
                    pos++
                    tokens.add(Token("STR", src.substring(start, pos), sb.toString()))
                    return
                } else {
                    sb.append(ch); pos++
                }
            }
            tokens.add(Token("STR", src.substring(start), sb.toString()))
        }

        private fun readNumber() {
            val start = pos
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
            val text = src.substring(start, pos)
            tokens.add(Token("NUM", text, text.toFloat()))
        }

        private fun readIdentifier() {
            val start = pos
            while (pos < src.length && (src[pos].isLetterOrDigit() || src[pos] == '_')) pos++
            val text = src.substring(start, pos)
            tokens.add(Token("IDENT", text, text))
        }

        private fun readOperator() {
            val two = if (pos + 1 < src.length) src.substring(pos, pos + 2) else ""
            val op = when (two) {
                "==", "!=", "<=", ">=", "&&", "||", "+=", "-=", "*=", "/=", ">=" -> { pos += 2; two }
                else -> {
                    val c = src[pos].toString(); pos++; c
                }
            }
            tokens.add(Token("OP", op, op))
        }
    }

    // ------------------------------------------------------------------
    //  Parser
    // ------------------------------------------------------------------
    class Parser(private val tokens: List<Lexer.Token>) {
        private var i = 0
        private val keywords = setOf("func", "var", "if", "else", "while", "for", "in", "return",
            "true", "false", "null", "and", "or", "not", "print", "break", "continue")

        fun parse(): List<Stmt> {
            val stmts = mutableListOf<Stmt>()
            while (!at("EOF")) {
                if (atSemi()) { i++; continue }
                statement()?.let { stmts.add(it) }
            }
            return stmts
        }

        private fun current() = tokens[i]
        private fun at(type: String) = current().type == type || current().value == type
        private fun atSemi() = current().type == "SEMI"
        private fun advance() = tokens[i++]
        private fun check(kw: String) = current().type == "IDENT" && current().text == kw

        private fun statement(): Stmt? {
            val tok = current()
            return when {
                tok.type == "IDENT" && tok.text == "func" -> function()
                tok.type == "IDENT" && tok.text == "var" -> varDecl()
                tok.type == "IDENT" && tok.text == "if" -> ifStmt()
                tok.type == "IDENT" && tok.text == "while" -> whileStmt()
                tok.type == "IDENT" && tok.text == "for" -> forStmt()
                tok.type == "IDENT" && tok.text == "return" -> returnStmt()
                tok.type == "IDENT" && tok.text == "break" || tok.type == "IDENT" && tok.text == "continue" -> { i++; null }
                else -> expressionStatement()
            }
        }

        private fun function(): Stmt {
            i++ // func
            val name = expectIdent()
            expectVal("(")
            val params = mutableListOf<String>()
            if (!at(")")) {
                do { params.add(expectIdent()) } while (consume(","))
            }
            expectVal(")")
            val body = block()
            return FuncStmt(name, params, body)
        }

        private fun varDecl(): Stmt {
            i++ // var
            val name = expectIdent()
            var init: Expr? = null
            if (consume("=")) init = expression()
            return VarStmt(name, init)
        }

        private fun ifStmt(): Stmt {
            i++ // if
            val cond = expression()
            val then = block()
            while (atSemi()) i++ // skip separators (braceify puts `}` then `else` on next line)
            val otherwise = if (check("else")) {
                i++
                while (atSemi()) i++
                block()
            } else emptyList()
            return IfStmt(cond, then, otherwise)
        }

        private fun whileStmt(): Stmt {
            i++
            val cond = expression()
            val body = block()
            return WhileStmt(cond, body)
        }

        private fun forStmt(): Stmt {
            i++
            val varName = expectIdent()
            expectVal("in")
            val iter = expression()
            val body = block()
            return ForStmt(varName, iter, body)
        }

        private fun returnStmt(): Stmt {
            i++
            val value = if (atSemi() || at("EOF") || check("}")) null else expression()
            return ReturnStmt(value)
        }

        private fun expressionStatement(): Stmt? {
            if (atSemi()) { i++; return null }
            if (at("}") || at("EOF")) return null
            val expr = expression()
            return ExprStmt(expr)
        }

        private fun block(): List<Stmt> {
            expectVal("{")
            val stmts = mutableListOf<Stmt>()
            while (!at("}") && !at("EOF")) {
                if (atSemi()) { i++; continue }
                statement()?.let { stmts.add(it) }
            }
            expectVal("}")
            return stmts
        }

        private fun expression(): Expr {
            var left = assignment()
            while (true) {
                val tok = current()
                if (tok.type == "OP" && tok.text in setOf("==", "!=", "<", ">", "<=", ">=")) {
                    i++
                    left = BinExpr(tok.text, left, expression())
                } else break
            }
            return left
        }

        private fun assignment(): Expr {
            var left = logicalOr()
            val compound = compoundOp()
            if (compound != null) {
                val value = assignment()
                return CompoundAssignExpr(left, compound, value)
            }
            if (consume("=")) {
                val value = assignment()
                if (left is VarExpr) return AssignExpr(left, value)
                if (left is MemberExpr) return AssignExpr(left, value)
                if (left is IndexExpr) return AssignExpr(left, value)
            }
            return left
        }

        private fun compoundOp(): String? {
            val t = current()
            if (t.type == "OP" && t.value in setOf("+=", "-=", "*=", "/=")) {
                i++
                return t.value.toString()
            }
            return null
        }

        private fun logicalOr(): Expr {
            var left = logicalAnd()
            while (at("||") || check("or")) {
                i++
                left = BinExpr("||", left, logicalAnd())
            }
            return left
        }

        private fun logicalAnd(): Expr {
            var left = equality()
            while (at("&&") || check("and")) {
                i++
                left = BinExpr("&&", left, equality())
            }
            return left
        }

        private fun equality(): Expr {
            var left = comparison()
            while (at("==") || at("!=")) {
                val op = advance().text
                left = BinExpr(op, left, comparison())
            }
            return left
        }

        private fun comparison(): Expr {
            var left = additive()
            while (at("<") || at(">") || at("<=") || at(">=")) {
                val op = advance().text
                left = BinExpr(op, left, additive())
            }
            return left
        }

        private fun additive(): Expr {
            var left = multiplicative()
            while (at("+") || at("-")) {
                val op = advance().text
                left = BinExpr(op, left, multiplicative())
            }
            return left
        }

        private fun multiplicative(): Expr {
            var left = unary()
            while (at("*") || at("/") || at("%")) {
                val op = advance().text
                left = BinExpr(op, left, unary())
            }
            return left
        }

        private fun unary(): Expr {
            if (at("-")) { i++; return UnaryExpr("-", unary()) }
            if (at("!")) { i++; return UnaryExpr("!", unary()) }
            if (check("not")) { i++; return UnaryExpr("!", unary()) }
            return primary()
        }

        private fun primary(): Expr {
            val tok = current()
            return when {
                tok.type == "NUM" -> { i++; Literal(tok.value) }
                tok.type == "STR" -> { i++; Literal(tok.value) }
                tok.type == "IDENT" && tok.text == "true" -> { i++; Literal(true) }
                tok.type == "IDENT" && tok.text == "false" -> { i++; Literal(false) }
                tok.type == "IDENT" && tok.text == "null" -> { i++; Literal(null) }
                tok.type == "IDENT" && tok.text == "self" -> { i++; SelfExpr() }
                tok.type == "IDENT" -> { i++; postfix(VarExpr(tok.text)) }
                tok.type == "(" -> {
                    i++
                    val e = expression()
                    expectVal(")")
                    postfix(e)
                }
                tok.type == "[" -> {
                    i++
                    val items = mutableListOf<Expr>()
                    if (!at("]")) {
                        do { items.add(expression()) } while (consume(","))
                    }
                    expectVal("]")
                    ArrayLit(items)
                }
                else -> { i++; Literal(null) }
            }
        }

        private fun postfix(expr: Expr): Expr {
            var e = expr
            while (true) {
                when {
                    at(".") -> {
                        i++
                        val name = expectIdent()
                        if (at("(")) { // method call
                            i++
                            val args = argList()
                            e = CallExpr(MemberExpr(e, name), args)
                        } else {
                            e = MemberExpr(e, name)
                        }
                    }
                    at("(") -> {
                        i++
                        val args = argList()
                        e = CallExpr(e, args)
                    }
                    at("[") -> {
                        i++
                        val idx = expression()
                        expectVal("]")
                        e = IndexExpr(e, idx)
                    }
                    else -> return e
                }
            }
        }

        private fun argList(): List<Expr> {
            val args = mutableListOf<Expr>()
            if (!at(")")) {
                do { args.add(expression()) } while (consume(","))
            }
            expectVal(")")
            return args
        }

        private fun expectIdent(): String {
            if (current().type == "IDENT") return advance().text
            return advance().text
        }

        private fun expectVal(v: String) {
            if (at(v)) i++ else i++
        }

        private fun consume(v: String): Boolean {
            if (at(v)) { i++; return true }
            return false
        }
    }

    // ------------------------------------------------------------------
    //  Interpreter
    // ------------------------------------------------------------------
    class Env(private val parent: Env? = null) {
        private val values = HashMap<String, Any?>()
        fun set(name: String, value: Any?) { values[name] = value }
        fun get(name: String): Any? {
            if (values.containsKey(name)) return values[name]
            return parent?.get(name)
        }
        fun has(name: String): Boolean = values.containsKey(name) || (parent?.has(name) ?: false)
        fun assign(name: String, value: Any?) {
            if (values.containsKey(name)) values[name] = value
            else parent?.assign(name, value) ?: set(name, value)
        }
    }

    class Interpreter(private val node: Node, private val env: Env) {

        fun executeBlock(stmts: List<Stmt>) {
            for (s in stmts) {
                execute(s)
            }
        }

        private fun execute(stmt: Stmt) {
            when (stmt) {
                is VarStmt -> env.set(stmt.name, stmt.init?.let { eval(it) })
                is AssignStmt -> assignValue(stmt.target, eval(stmt.value))
                is ExprStmt -> eval(stmt.expr)
                is IfStmt -> {
                    if (truthy(eval(stmt.cond))) executeBlock(stmt.then)
                    else executeBlock(stmt.otherwise)
                }
                is WhileStmt -> {
                    var guard = 0
                    while (truthy(eval(stmt.cond))) {
                        executeBlock(stmt.body)
                        if (++guard > 1_000_000) break
                    }
                }
                is ForStmt -> {
                    val iter = eval(stmt.iter)
                    val seq = when (iter) {
                        is List<*> -> iter
                        is IntRange -> iter.map { it }
                        is Number -> (0 until iter.toInt()).map { it.toFloat() }
                        else -> emptyList<Any?>()
                    }
                    val loopEnv = Env(env)
                    for (item in seq) {
                        loopEnv.set(stmt.varName, item)
                        Interpreter(node, loopEnv).executeBlock(stmt.body)
                    }
                }
                is FuncStmt -> env.set(stmt.name, MDScriptFunction(stmt.params, stmt.body, env))
                is ReturnStmt -> throw RuntimeReturn(stmt.value?.let { eval(it) })
            }
        }

        private fun assignValue(target: Expr, value: Any?) {
            when (target) {
                is VarExpr -> env.assign(target.name, value)
                is MemberExpr -> {
                    val obj = eval(target.obj)
                    setMember(obj, target.name, value)
                }
                is IndexExpr -> {
                    val obj = eval(target.obj)
                    val idx = eval(target.index)
                    if (obj is MutableList<*> && idx is Number) {
                        @Suppress("UNCHECKED_CAST")
                        (obj as MutableList<Any?>)[idx.toInt()] = value
                    }
                }
                else -> {}
            }
        }

        private fun setMember(obj: Any?, name: String, value: Any?) {
            when (obj) {
                is Node -> obj.setProperty(name, value)
                is MutableMap<*, *> -> @Suppress("UNCHECKED_CAST") (obj as MutableMap<String, Any?>)[name] = value
                else -> {}
            }
        }

        private fun eval(expr: Expr): Any? {
            return when (expr) {
                is Literal -> expr.value
                is SelfExpr -> node
                is VarExpr -> resolveVar(expr.name)
                is ArrayLit -> expr.items.map { eval(it) }
                is UnaryExpr -> {
                    val v = eval(expr.operand)
                    when (expr.op) {
                        "-" -> if (v is Number) -v.toFloat() else v
                        "!" -> !truthy(v)
                        else -> v
                    }
                }
                is BinExpr -> evalBinary(expr.op, eval(expr.left), eval(expr.right))
                is MemberExpr -> getMember(eval(expr.obj), expr.name)
                is IndexExpr -> {
                    val obj = eval(expr.obj)
                    val idx = eval(expr.index)
                    when {
                        obj is List<*> && idx is Number -> obj.getOrNull(idx.toInt())
                        obj is Map<*, *> -> obj[idx]
                        obj is String && idx is Number -> obj.getOrNull(idx.toInt())?.toString()
                        else -> null
                    }
                }
                is CallExpr -> evalCall(expr)
                is AssignExpr -> assignValue(expr.target, eval(expr.value))
                is CompoundAssignExpr -> {
                    val current = eval(expr.target)
                    val rhs = eval(expr.value)
                    val op = expr.op.dropLast(1) // "+=" -> "+"
                    val result = evalBinary(op, current, rhs)
                    assignValue(expr.target, result)
                    result
                }
            }
        }

        private fun resolveVar(name: String): Any? {
            return when (name) {
                "Input" -> com.agentgame.one.engine.input.Input
                "PI" -> PI.toFloat()
                "print" -> null
                else -> env.get(name)
            }
        }

        private fun getMember(obj: Any?, name: String): Any? {
            return when (obj) {
                is Node -> obj.getProperty(name)
                is Vector2 -> when (name) {
                    "x" -> obj.x
                    "y" -> obj.y
                    "length" -> obj.length
                    "normalized" -> obj.normalized()
                    else -> null
                }
                is Map<*, *> -> obj[name]
                is List<*> -> when (name) {
                    "size", "length" -> obj.size
                    else -> null
                }
                is String -> when (name) {
                    "length" -> obj.length
                    else -> null
                }
                else -> null
            }
        }

        private fun evalBinary(op: String, l: Any?, r: Any?): Any? {
            val a = l
            val b = r
            return when (op) {
                "+" -> when {
                    a is Vector2 && b is Vector2 -> a + b
                    a is Vector2 && b is Number -> Vector2(a.x + b.toFloat(), a.y + b.toFloat())
                    a is Number && b is Vector2 -> Vector2(a.toFloat() + b.x, a.toFloat() + b.y)
                    a is Number && b is Number -> a.toFloat() + b.toFloat()
                    a is String || b is String -> "$a$b"
                    a is List<*> && b is List<*> -> a + b
                    else -> null
                }
                "-" -> when {
                    a is Vector2 && b is Vector2 -> a - b
                    a is Vector2 && b is Number -> Vector2(a.x - b.toFloat(), a.y - b.toFloat())
                    a is Number && b is Number -> a.toFloat() - b.toFloat()
                    else -> null
                }
                "*" -> when {
                    a is Vector2 && b is Number -> a * b.toFloat()
                    a is Vector2 && b is Vector2 -> a * b
                    a is Number && b is Vector2 -> b * a.toFloat()
                    a is Number && b is Number -> a.toFloat() * b.toFloat()
                    else -> null
                }
                "/" -> when {
                    a is Vector2 && b is Number -> Vector2(a.x / b.toFloat(), a.y / b.toFloat())
                    a is Number && b is Number && b.toFloat() != 0f -> a.toFloat() / b.toFloat()
                    else -> null
                }
                "%" -> if (a is Number && b is Number && b.toFloat() != 0f) a.toFloat() % b.toFloat() else null
                "==" -> deepEquals(a, b)
                "!=" -> !deepEquals(a, b)
                "<" -> if (a is Number && b is Number) a.toFloat() < b.toFloat() else null
                ">" -> if (a is Number && b is Number) a.toFloat() > b.toFloat() else null
                "<=" -> if (a is Number && b is Number) a.toFloat() <= b.toFloat() else null
                ">=" -> if (a is Number && b is Number) a.toFloat() >= b.toFloat() else null
                "&&" -> truthy(a) && truthy(b)
                "||" -> truthy(a) || truthy(b)
                else -> null
            }
        }

        private fun deepEquals(a: Any?, b: Any?): Boolean {
            if (a === b) return true
            if (a is Number && b is Number) return a.toFloat() == b.toFloat()
            if (a is Vector2 && b is Vector2) return a == b
            if (a is Boolean && b is Boolean) return a == b
            if (a is String && b is String) return a == b
            if (a == null) return b == null
            return false
        }

        private fun truthy(v: Any?): Boolean {
            return when (v) {
                null -> false
                is Boolean -> v
                is Number -> v.toFloat() != 0f
                is String -> v.isNotEmpty()
                is List<*> -> v.isNotEmpty()
                else -> true
            }
        }

        private fun evalCall(expr: CallExpr): Any? {
            val args = expr.args.map { eval(it) }
            val callee = expr.callee
            return when (callee) {
                is VarExpr -> {
                    val name = callee.name
                    when (name) {
                        "print" -> {
                            val text = args.joinToString(" ") { it?.toString() ?: "null" }
                            MDScriptLog.log(text)
                            null
                        }
                        "Vector2" -> {
                            val x = (args.getOrNull(0) as? Number)?.toFloat() ?: 0f
                            val y = (args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                            Vector2(x, y)
                        }
                        else -> {
                            val fn = env.get(name) as? MDScriptFunction
                            if (fn != null) callFunction(fn, args)
                            else invokeNodeMethod(node, name, args)
                        }
                    }
                }
                is MemberExpr -> {
                    val obj = eval(callee.obj)
                    when (obj) {
                        is Node -> invokeNodeMethod(obj, callee.name, args)
                        is com.agentgame.one.engine.input.Input -> {
                            invokeInput(callee.name, args)
                        }
                        is Map<*, *> -> {
                            val fn = obj[callee.name] as? MDScriptFunction
                            fn?.let { return callFunction(it, args) } ?: invokeKotlin(obj, callee.name, args)
                        }
                        is List<*> -> when (callee.name) {
                            "add" -> { @Suppress("UNCHECKED_CAST") (obj as MutableList<Any?>).add(args.getOrNull(0)); null }
                            "append" -> { @Suppress("UNCHECKED_CAST") (obj as MutableList<Any?>).add(args.getOrNull(0)); null }
                            "remove" -> { @Suppress("UNCHECKED_CAST") (obj as MutableList<Any?>).remove(args.getOrNull(0)); null }
                            "size" -> obj.size
                            else -> invokeKotlin(obj, callee.name, args)
                        }
                        else -> invokeKotlin(obj, callee.name, args)
                    }
                }
                else -> {
                    // bare call e.g. builtin function stored in env
                    val fn = env.get("__call") as? MDScriptFunction
                    if (fn != null) callFunction(fn, args) else null
                }
            }
        }

        private fun invokeInput(name: String, args: List<Any?>): Any? {
            val input = com.agentgame.one.engine.input.Input
            return when (name) {
                "isActionPressed" -> if (args.isNotEmpty()) input.isActionPressed(args[0]?.toString() ?: "") else false
                "isActionJustPressed" -> if (args.isNotEmpty()) input.isActionJustPressed(args[0]?.toString() ?: "") else false
                "isActionJustReleased" -> if (args.isNotEmpty()) input.isActionJustReleased(args[0]?.toString() ?: "") else false
                "actionPress" -> { if (args.isNotEmpty()) input.actionPress(args[0]?.toString() ?: ""); null }
                "actionRelease" -> { if (args.isNotEmpty()) input.actionRelease(args[0]?.toString() ?: ""); null }
                "joystickVector" -> {
                    input.joystickVector(args.getOrNull(0) as? Int)
                }
                else -> null
            }
        }

        private fun callFunction(fn: MDScriptFunction, args: List<Any?>) {
            val fnEnv = Env(fn.closure)
            bindParams(fn, args.toTypedArray(), fnEnv)
            try {
                Interpreter(node, fnEnv).executeBlock(fn.body)
            } catch (r: RuntimeReturn) {
                // swallowed: return handled via closure holder if needed
            }
        }

        private fun invokeNodeMethod(target: Node, name: String, args: List<Any?>): Any? {
            return try {
                val kClass = target.javaClass.kotlin
                var k: kotlin.reflect.KClass<*>? = kClass
                while (k != null) {
                    val method = k.memberFunctions.firstOrNull { it.name == name }
                    if (method != null) {
                        return method.call(target, *args.map { convertArg(it, method) }.toTypedArray())
                    }
                    k = k.superclass
                }
                null
            } catch (t: Throwable) {
                null
            }
        }

        private fun convertArg(v: Any?, method: kotlin.reflect.KFunction<*>): Any? {
            if (v == null) return null
            val param = method.parameters.firstOrNull { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            val type = param?.type?.classifier
            return when (type) {
                Float::class -> (v as? Number)?.toFloat()
                Int::class -> (v as? Number)?.toInt()
                Double::class -> (v as? Number)?.toDouble()
                Boolean::class -> v as? Boolean
                else -> v
            }
        }

        private fun invokeKotlin(obj: Any?, name: String, args: List<Any?>): Any? {
            if (obj == null) return null
            return try {
                val methods = obj.javaClass.methods
                val m = methods.firstOrNull { it.name == name }
                if (m != null) m.invoke(obj, *args.toTypedArray()) else null
            } catch (t: Throwable) { null }
        }
    }

    private fun bindParams(fn: MDScriptFunction, args: Array<Any?>, env: Env) {
        for ((idx, p) in fn.params.withIndex()) {
            env.set(p, args.getOrNull(idx))
        }
    }
}

/** Extension: attach an MDScript source to a node. */
fun Node.attachScript(source: String): Node {
    val s = MDScript.compile(this, source)
    this.script = s
    return this
}

/** Global capture hook for MDScript `print`, used by the agent's `run` command. */
object MDScriptLog {
    var hook: (String) -> Unit = {}
    fun log(message: String) {
        android.util.Log.d("MDScript", message)
        hook(message)
    }
}

/** Helpers to run a script outside the full node lifecycle (agent command runner). */
object MDScriptDebug {
    /**
     * Compiles and runs a script's top-level statements once. Scripts that only define
     * `_ready`-style callbacks won't self-execute; call this for print()-heavy scripts.
     */
    fun runMain(node: Node, source: String) {
        val script = MDScript.compile(node, source)
        node.script = script
        // Running top-level statements is handled by a one-shot interpreter pass.
        val env = com.agentgame.one.engine.scripting.MDScript.Env()
        env.set("self", node)
        com.agentgame.one.engine.scripting.MDScript.Interpreter(node, env)
            .executeBlock(script.programForDebug())
    }
}

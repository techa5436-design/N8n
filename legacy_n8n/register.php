<?php
require 'db.php';

// Block access if admin already exists
if (hasAdmin($pdo)) {
    die("<h1>Access Denied</h1><p>An administrator account already exists. <a href='login.php'>Click here to login</a>.</p>");
}

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';

    if (!empty($username) && !empty($password)) {
        $hashed = password_hash($password, PASSWORD_DEFAULT);
        $stmt = $pdo->prepare("INSERT INTO users (username, password) VALUES (?, ?)");
        $stmt->execute([$username, $hashed]);

        $_SESSION['admin_logged_in'] = true;
        $_SESSION['username'] = $username;
        header("Location: dashboard.php");
        exit;
    } else {
        $error = "Please fill in all fields.";
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>First-Time Setup - Register Admin</title>
    <style>
        body { font-family: sans-serif; display: grid; place-items: center; min-height: 100vh; background: #f4f4f5; margin: 0; }
        .card { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); width: 320px; }
        input, button { width: 100%; padding: 10px; margin-top: 10px; box-sizing: border-box; }
        button { background: #2563eb; color: white; border: none; border-radius: 4px; cursor: pointer; }
        .error { color: red; font-size: 0.9rem; }
    </style>
</head>
<body>
    <div class="card">
        <h2>Initial Admin Setup</h2>
        <p><small>This account will be the sole administrator.</small></p>
        <?php if ($error): ?><p class="error"><?= htmlspecialchars($error) ?></p><?php endif; ?>
        <form method="POST">
            <input type="text" name="username" placeholder="Admin Username" required>
            <input type="password" name="password" placeholder="Password" required>
            <button type="submit">Create Admin Account</button>
        </form>
    </div>
</body>
</html>
<?php
require 'db.php';

if (empty($_SESSION['admin_logged_in'])) {
    header("Location: login.php");
    exit;
}

// Add Project
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['add_project'])) {
    $title = trim($_POST['title']);
    $description = trim($_POST['description']);
    $image_url = trim($_POST['image_url']);
    $link = trim($_POST['link']);

    if (!empty($title) && !empty($description)) {
        $stmt = $pdo->prepare("INSERT INTO projects (title, description, image_url, link) VALUES (?, ?, ?, ?)");
        $stmt->execute([$title, $description, $image_url, $link]);
        header("Location: dashboard.php");
        exit;
    }
}

// Delete Project
if (isset($_GET['delete'])) {
    $stmt = $pdo->prepare("DELETE FROM projects WHERE id = ?");
    $stmt->execute([$_GET['delete']]);
    header("Location: dashboard.php");
    exit;
}

$projects = $pdo->query("SELECT * FROM projects ORDER BY id DESC")->fetchAll();
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Admin Dashboard</title>
    <style>
        body { font-family: sans-serif; max-width: 900px; margin: 2rem auto; padding: 0 1rem; background: #fafafa; }
        .header { display: flex; justify-content: space-between; align-items: center; }
        .card { background: white; padding: 1.5rem; border-radius: 6px; box-shadow: 0 2px 6px rgba(0,0,0,0.05); margin-bottom: 2rem; }
        input, textarea { width: 100%; padding: 8px; margin: 6px 0 12px; box-sizing: border-box; }
        button { background: #2563eb; color: white; border: none; padding: 10px 16px; border-radius: 4px; cursor: pointer; }
        table { width: 100%; border-collapse: collapse; background: white; }
        th, td { padding: 12px; border-bottom: 1px solid #ddd; text-align: left; }
        .del { color: #dc2626; text-decoration: none; font-weight: bold; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Welcome, <?= htmlspecialchars($_SESSION['username']) ?></h1>
        <p><a href="index.php" target="_blank">View Live Site</a> | <a href="login.php?action=logout">Logout</a></p>
    </div>

    <div class="card">
        <h3>Add New Project</h3>
        <form method="POST">
            <label>Project Title *</label>
            <input type="text" name="title" required>
            
            <label>Description *</label>
            <textarea name="description" rows="3" required></textarea>
            
            <label>Image URL (Optional)</label>
            <input type="url" name="image_url" placeholder="https://example.com/image.jpg">
            
            <label>Project Link (Optional)</label>
            <input type="url" name="link" placeholder="https://github.com/my-project">
            
            <button type="submit" name="add_project">Publish Project</button>
        </form>
    </div>

    <h3>Existing Projects</h3>
    <table>
        <thead>
            <tr>
                <th>Title</th>
                <th>Description</th>
                <th>Action</th>
            </tr>
        </thead>
        <tbody>
            <?php foreach ($projects as $p): ?>
            <tr>
                <td><strong><?= htmlspecialchars($p['title']) ?></strong></td>
                <td><?= htmlspecialchars($p['description']) ?></td>
                <td><a class="del" href="?delete=<?= $p['id'] ?>" onclick="return confirm('Delete this project?')">Delete</a></td>
            </tr>
            <?php endforeach; ?>
        </tbody>
    </table>
</body>
</html>
<?php
require 'db.php';

// Redirect to admin setup if not set up yet
if (!hasAdmin($pdo)) {
    header("Location: register.php");
    exit;
}

$projects = $pdo->query("SELECT * FROM projects ORDER BY id DESC")->fetchAll();
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Portfolio</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #111827; color: #f9fafb; margin: 0; padding: 0; }
        header { text-align: center; padding: 4rem 1rem 2rem; }
        h1 { font-size: 2.5rem; margin-bottom: 0.5rem; }
        p.subtitle { color: #9ca3af; font-size: 1.1rem; }
        .container { max-width: 1100px; margin: 0 auto; padding: 2rem 1rem; }
        .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1.5rem; }
        .card { background: #1f2937; border-radius: 8px; overflow: hidden; border: 1px solid #374151; display: flex; flex-direction: column; }
        .card img { width: 100%; height: 180px; object-fit: cover; }
        .card-body { padding: 1.25rem; flex: 1; display: flex; flex-direction: column; }
        .card-body h3 { margin-top: 0; }
        .card-body p { color: #d1d5db; flex: 1; font-size: 0.95rem; line-height: 1.5; }
        .card-body a { color: #60a5fa; text-decoration: none; font-weight: bold; margin-top: 1rem; display: inline-block; }
        footer { text-align: center; padding: 2rem; color: #6b7280; font-size: 0.85rem; }
    </style>
</head>
<body>
    <header>
        <h1>Developer Portfolio</h1>
        <p class="subtitle">A showcase of recent work and side projects</p>
    </header>

    <div class="container">
        <div class="grid">
            <?php if (empty($projects)): ?>
                <p style="grid-column: 1/-1; text-align: center; color: #9ca3af;">No projects added yet.</p>
            <?php endif; ?>

            <?php foreach ($projects as $p): ?>
                <div class="card">
                    <?php if ($p['image_url']): ?>
                        <img src="<?= htmlspecialchars($p['image_url']) ?>" alt="<?= htmlspecialchars($p['title']) ?>">
                    <?php endif; ?>
                    <div class="card-body">
                        <h3><?= htmlspecialchars($p['title']) ?></h3>
                        <p><?= nl2br(htmlspecialchars($p['description'])) ?></p>
                        <?php if ($p['link']): ?>
                            <a href="<?= htmlspecialchars($p['link']) ?>" target="_blank" rel="noopener">View Project &rarr;</a>
                        <?php endif; ?>
                    </div>
                </div>
            <?php endforeach; ?>
        </div>
    </div>

    <footer>
        <p>&copy; <?= date('Y') ?> Portfolio. Built with PHP &amp; MySQL.</p>
    </footer>
</body>
</html>
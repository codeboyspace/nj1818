const http = require('http');
const fs = require('fs');
const path = require('path');

const root = __dirname;
const port = 5600;

const mime = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

http.createServer((req, res) => {
  const reqPath = decodeURIComponent((req.url || '/').split('?')[0]);
  const safePath = reqPath === '/' ? '/index.html' : reqPath;
  const filePath = path.join(root, safePath);

  fs.stat(filePath, (statErr, stat) => {
    if (statErr) {
      res.statusCode = 404;
      res.end('Not found');
      return;
    }

    const target = stat.isDirectory() ? path.join(filePath, 'index.html') : filePath;
    fs.readFile(target, (readErr, data) => {
      if (readErr) {
        res.statusCode = 404;
        res.end('Not found');
        return;
      }
      res.setHeader('Content-Type', mime[path.extname(target).toLowerCase()] || 'application/octet-stream');
      res.end(data);
    });
  });
}).listen(port, () => {
  console.log(`Frontend server running at http://localhost:${port}`);
});



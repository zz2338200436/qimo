from __future__ import annotations

import http.client
import http.server
import mimetypes
import socketserver
from pathlib import Path
from urllib.parse import urlsplit


REPO_ROOT = Path(__file__).resolve().parent.parent
FRONTEND_ROOT = REPO_ROOT / "frontend" / "dist"
GATEWAY_HOST = "localhost"
GATEWAY_PORT = 8080


class FrontendProxyHandler(http.server.SimpleHTTPRequestHandler):
    def translate_path(self, path: str) -> str:
        request_path = urlsplit(path).path
        if request_path.startswith("/"):
            request_path = request_path[1:]
        return str(FRONTEND_ROOT / request_path)

    def do_GET(self) -> None:
        self._handle_request()

    def do_POST(self) -> None:
        self._handle_request()

    def do_PUT(self) -> None:
        self._handle_request()

    def do_DELETE(self) -> None:
        self._handle_request()

    def do_PATCH(self) -> None:
        self._handle_request()

    def do_OPTIONS(self) -> None:
        self._handle_request()

    def _handle_request(self) -> None:
        request_path = urlsplit(self.path).path
        if request_path.startswith("/api/"):
            self._proxy_to_gateway()
            return
        super().do_GET()

    def _proxy_to_gateway(self) -> None:
        body = None
        content_length = int(self.headers.get("Content-Length", "0"))
        if content_length > 0:
            body = self.rfile.read(content_length)

        connection = http.client.HTTPConnection(GATEWAY_HOST, GATEWAY_PORT, timeout=30)
        try:
            headers = {key: value for key, value in self.headers.items()}
            headers["Host"] = f"{GATEWAY_HOST}:{GATEWAY_PORT}"
            connection.request(self.command, self.path, body=body, headers=headers)
            response = connection.getresponse()
            payload = response.read()

            self.send_response(response.status, response.reason)
            excluded_headers = {"transfer-encoding", "connection", "keep-alive"}
            for key, value in response.getheaders():
                if key.lower() in excluded_headers:
                    continue
                self.send_header(key, value)
            if not any(key.lower() == "content-length" for key, _ in response.getheaders()):
                self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
        finally:
            connection.close()

    def guess_type(self, path: str) -> str:
        guessed, _ = mimetypes.guess_type(path)
        return guessed or "application/octet-stream"

    def log_message(self, format: str, *args) -> None:
        super().log_message(format, *args)


def main() -> None:
    with socketserver.ThreadingTCPServer(("0.0.0.0", 5500), FrontendProxyHandler) as httpd:
        httpd.serve_forever()


if __name__ == "__main__":
    main()

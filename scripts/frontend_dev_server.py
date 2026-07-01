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
    def end_headers(self) -> None:
        self.send_header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        super().end_headers()

    def translate_path(self, path: str) -> str:
        request_path = urlsplit(path).path
        if request_path.startswith("/"):
            request_path = request_path[1:]
        return str(FRONTEND_ROOT / request_path)

    def do_GET(self) -> None:
        self._handle_request()

    def do_HEAD(self) -> None:
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
        if self.command == "GET":
            super().do_GET()
            return
        if self.command == "HEAD":
            super().do_HEAD()
            return
        self.send_error(405, "Method Not Allowed")

    def _proxy_to_gateway(self) -> None:
        body = None
        content_length = int(self.headers.get("Content-Length", "0"))
        if content_length > 0:
            body = self.rfile.read(content_length)

        connection = http.client.HTTPConnection(GATEWAY_HOST, GATEWAY_PORT, timeout=90)
        try:
            headers = {key: value for key, value in self.headers.items()}
            headers["Host"] = f"{GATEWAY_HOST}:{GATEWAY_PORT}"
            connection.request(self.command, self.path, body=body, headers=headers)
            response = connection.getresponse()
            response_headers = response.getheaders()
            content_type = (response.getheader("Content-Type") or "").lower()
            is_sse = content_type.startswith("text/event-stream")
            payload = b""
            upstream_content_length = response.getheader("Content-Length")
            if not is_sse and self.command != "HEAD":
                payload = response.read()

            self.send_response(response.status, response.reason)
            excluded_headers = {"transfer-encoding", "connection", "keep-alive"}
            for key, value in response_headers:
                header_name = key.lower()
                if header_name in excluded_headers:
                    continue
                if not is_sse and header_name == "content-length":
                    continue
                self.send_header(key, value)
            if is_sse:
                self.send_header("X-Accel-Buffering", "no")
            else:
                if self.command == "HEAD":
                    content_length = upstream_content_length or "0"
                else:
                    content_length = str(len(payload))
                self.send_header("Content-Length", content_length)
            self.end_headers()
            if is_sse:
                reader = getattr(response, "read1", None)
                if reader is None and getattr(response, "fp", None) is not None:
                    reader = getattr(response.fp, "read1", None)
                while True:
                    chunk = reader(4096) if reader else response.readline()
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    self.wfile.flush()
            elif self.command != "HEAD":
                self.wfile.write(payload)
        finally:
            connection.close()

    def guess_type(self, path: str) -> str:
        guessed, _ = mimetypes.guess_type(path)
        return guessed or "application/octet-stream"

    def log_message(self, format: str, *args) -> None:
        super().log_message(format, *args)


class ReusableThreadingTCPServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True


def main() -> None:
    with ReusableThreadingTCPServer(("0.0.0.0", 5500), FrontendProxyHandler) as httpd:
        httpd.serve_forever()


if __name__ == "__main__":
    main()

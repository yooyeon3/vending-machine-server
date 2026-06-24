import sys
import io
import threading
import time
import requests
import qrcode
from datetime import datetime

from PyQt5.QtWidgets import (
    QApplication, QMainWindow, QWidget, QStackedWidget,
    QLabel, QPushButton, QVBoxLayout, QHBoxLayout, QFrame
)
from PyQt5.QtCore import Qt, QTimer, pyqtSignal, QObject
from PyQt5.QtGui import QFont, QPixmap, QImage, QColor, QPalette

SERVER = "http://192.168.0.171:8080"

PRODUCTS = [
    {"name": "펩시 콜라",         "price": 1500},
    {"name": "레쓰비 마일드 커피", "price": 1200},
]

# ── 스타일 ─────────────────────────────────────────────────
STYLE = """
QWidget { background-color: #faf8f4; font-family: 'Nanum Gothic', 'Malgun Gothic', sans-serif; }

#header {
    background-color: #3d2c1e;
    padding: 16px;
}
#header QLabel {
    color: white;
    font-size: 22px;
    font-weight: bold;
    background: transparent;
}

#headerGreen { background-color: #22c55e; padding: 16px; }
#headerGreen QLabel { color: white; font-size: 22px; font-weight: bold; background: transparent; }

#headerMoving { background-color: #c47d4a; padding: 16px; }
#headerMoving QLabel { color: white; font-size: 22px; font-weight: bold; background: transparent; }

.drink-btn {
    background-color: white;
    border: 2px solid #e8dfd4;
    border-radius: 20px;
    padding: 20px;
    font-size: 18px;
    font-weight: bold;
    color: #3d2c1e;
    min-width: 220px;
    min-height: 160px;
}
.drink-btn:hover { background-color: #f2ede4; border-color: #c47d4a; }
.drink-btn:pressed { background-color: #e8dfd4; }

#pin-box {
    background: white;
    border: 2px solid #e8dfd4;
    border-radius: 20px;
    padding: 24px;
}
#pin-label { font-size: 14px; color: #9e8c7a; background: transparent; }
#pin-value { font-size: 42px; font-weight: bold; color: #3d2c1e; background: transparent; letter-spacing: 4px; }
#pin-hint  { font-size: 13px; color: #9e8c7a; background: transparent; }

#product-label { font-size: 16px; color: #c47d4a; font-weight: bold; background: transparent; }
#guide-label   { font-size: 13px; color: #9e8c7a; background: transparent; }

#map-placeholder {
    background: #f2ede4;
    border: 2px solid #e8dfd4;
    border-radius: 16px;
    min-height: 280px;
}
#map-placeholder QLabel { color: #9e8c7a; font-size: 18px; background: transparent; }

#toast {
    background-color: #3d2c1e;
    color: white;
    border-radius: 12px;
    padding: 12px 24px;
    font-size: 15px;
    font-weight: bold;
}
"""

# ── 폴링 신호 ──────────────────────────────────────────────
class Poller(QObject):
    state_changed = pyqtSignal(str, object)   # (state, order)

    def __init__(self):
        super().__init__()
        self._running = True

    def start(self):
        t = threading.Thread(target=self._loop, daemon=True)
        t.start()

    def _loop(self):
        while self._running:
            try:
                self._poll()
            except Exception as e:
                print("poll error:", e)
            time.sleep(3)

    def _poll(self):
        r = requests.get(f"{SERVER}/api/orders/pending", timeout=5)
        orders = r.json()

        if not orders:
            self.state_changed.emit("IDLE", None)
            return

        order = orders[-1]
        purchase_time = order.get("purchaseTime", "")

        try:
            pt = datetime.fromisoformat(purchase_time)
            elapsed = (datetime.now() - pt).total_seconds()
            arrived = elapsed >= 60
        except Exception:
            arrived = False

        if arrived:
            self.state_changed.emit("ARRIVED", order)
            try:
                requests.post(f"{SERVER}/api/orders/{order['id']}/status",
                              json={"status": "DELIVERING"}, timeout=3)
            except Exception:
                pass
        else:
            self.state_changed.emit("MOVING", order)


# ── IDLE 화면 ──────────────────────────────────────────────
class IdlePage(QWidget):
    purchase_requested = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        # 헤더
        header = QWidget(); header.setObjectName("header")
        hl = QHBoxLayout(header)
        title = QLabel("PIMTO 자판기"); title.setAlignment(Qt.AlignCenter)
        hl.addWidget(title)
        layout.addWidget(header)

        # 음료 버튼
        body = QWidget()
        bl = QVBoxLayout(body)
        bl.setContentsMargins(40, 40, 40, 20)
        bl.setSpacing(20)

        btn_row = QHBoxLayout()
        btn_row.setSpacing(40)
        for p in PRODUCTS:
            btn = QPushButton(f"{p['name']}\n\n{p['price']:,}원\n\n터치하여 구매")
            btn.setProperty("class", "drink-btn")
            btn.setStyleSheet("""
                QPushButton {
                    background-color: white; border: 2px solid #e8dfd4;
                    border-radius: 20px; padding: 20px;
                    font-size: 18px; font-weight: bold; color: #3d2c1e;
                    min-width: 220px; min-height: 160px;
                }
                QPushButton:hover { background-color: #f2ede4; border-color: #c47d4a; }
                QPushButton:pressed { background-color: #e8dfd4; }
            """)
            name = p["name"]
            btn.clicked.connect(lambda _, n=name: self.purchase_requested.emit(n))
            btn_row.addWidget(btn)

        bl.addStretch()
        bl.addLayout(btn_row)
        bl.addStretch()

        guide = QLabel("앱/웹에서도 주문 가능합니다")
        guide.setObjectName("guide-label")
        guide.setAlignment(Qt.AlignCenter)
        bl.addWidget(guide)

        layout.addWidget(body)

        # 토스트
        self.toast = QLabel("", self)
        self.toast.setObjectName("toast")
        self.toast.setAlignment(Qt.AlignCenter)
        self.toast.hide()
        self._toast_timer = QTimer()
        self._toast_timer.timeout.connect(self.toast.hide)

    def show_toast(self, msg):
        self.toast.setText(msg)
        self.toast.adjustSize()
        self.toast.move(
            (self.width() - self.toast.width()) // 2,
            self.height() - self.toast.height() - 30
        )
        self.toast.show()
        self._toast_timer.start(2000)

    def resizeEvent(self, e):
        if self.toast.isVisible():
            self.toast.move(
                (self.width() - self.toast.width()) // 2,
                self.height() - self.toast.height() - 30
            )


# ── MOVING 화면 ────────────────────────────────────────────
class MovingPage(QWidget):
    def __init__(self):
        super().__init__()
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        header = QWidget(); header.setObjectName("headerMoving")
        hl = QHBoxLayout(header)
        title = QLabel("배달 중..."); title.setAlignment(Qt.AlignCenter)
        hl.addWidget(title)
        layout.addWidget(header)

        body = QWidget()
        bl = QVBoxLayout(body)
        bl.setContentsMargins(40, 30, 40, 20)
        bl.setSpacing(16)

        # 지도 플레이스홀더
        map_ph = QWidget(); map_ph.setObjectName("map-placeholder")
        ml = QVBoxLayout(map_ph)
        map_label = QLabel("로봇 이동 중\n\n(지도 연동 준비 중)")
        map_label.setAlignment(Qt.AlignCenter)
        ml.addWidget(map_label)
        bl.addWidget(map_ph, stretch=1)

        self.product_label = QLabel("")
        self.product_label.setObjectName("product-label")
        self.product_label.setAlignment(Qt.AlignCenter)
        bl.addWidget(self.product_label)

        layout.addWidget(body, stretch=1)

    def set_order(self, order):
        if order:
            self.product_label.setText(order.get("productName", ""))


# ── ARRIVED 화면 ───────────────────────────────────────────
class ArrivedPage(QWidget):
    def __init__(self):
        super().__init__()
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        header = QWidget(); header.setObjectName("headerGreen")
        hl = QHBoxLayout(header)
        title = QLabel("로봇이 도착했습니다!"); title.setAlignment(Qt.AlignCenter)
        hl.addWidget(title)
        layout.addWidget(header)

        body = QWidget()
        bl = QHBoxLayout(body)
        bl.setContentsMargins(40, 30, 40, 20)
        bl.setSpacing(40)

        # QR
        self.qr_label = QLabel()
        self.qr_label.setFixedSize(200, 200)
        self.qr_label.setAlignment(Qt.AlignCenter)
        self.qr_label.setStyleSheet("background: white; border: 2px solid #e8dfd4; border-radius: 12px;")
        bl.addWidget(self.qr_label, alignment=Qt.AlignVCenter)

        # PIN
        pin_box = QWidget(); pin_box.setObjectName("pin-box")
        pl = QVBoxLayout(pin_box)
        pl.setSpacing(12)

        pin_lbl = QLabel("PIN 번호"); pin_lbl.setObjectName("pin-label")
        self.pin_value = QLabel("----"); self.pin_value.setObjectName("pin-value")
        pin_hint = QLabel("웹/앱에서 입력해주세요"); pin_hint.setObjectName("pin-hint")
        self.product_label = QLabel(""); self.product_label.setObjectName("product-label")

        pl.addStretch()
        pl.addWidget(pin_lbl)
        pl.addWidget(self.pin_value)
        pl.addWidget(pin_hint)
        pl.addSpacing(16)
        pl.addWidget(self.product_label)
        pl.addStretch()

        bl.addWidget(pin_box, stretch=1, alignment=Qt.AlignVCenter)
        layout.addWidget(body, stretch=1)

    def set_order(self, order):
        if not order:
            return
        pin = order.get("pinCode", "----")
        self.pin_value.setText(pin)
        self.product_label.setText(order.get("productName", ""))
        self._update_qr(pin)

    def _update_qr(self, pin):
        try:
            qr = qrcode.QRCode(box_size=6, border=2)
            qr.add_data(pin)
            qr.make(fit=True)
            img = qr.make_image(fill_color="black", back_color="white")
            buf = io.BytesIO()
            img.save(buf, format="PNG")
            buf.seek(0)
            qimg = QImage.fromData(buf.getvalue())
            pix = QPixmap.fromImage(qimg).scaled(
                196, 196, Qt.KeepAspectRatio, Qt.SmoothTransformation)
            self.qr_label.setPixmap(pix)
        except Exception as e:
            print("QR error:", e)


# ── 메인 윈도우 ────────────────────────────────────────────
class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("PIMTO")
        self.showFullScreen()

        self.stack = QStackedWidget()
        self.setCentralWidget(self.stack)

        self.idle_page    = IdlePage()
        self.moving_page  = MovingPage()
        self.arrived_page = ArrivedPage()

        self.stack.addWidget(self.idle_page)    # 0
        self.stack.addWidget(self.moving_page)  # 1
        self.stack.addWidget(self.arrived_page) # 2

        self.idle_page.purchase_requested.connect(self.on_direct_purchase)

        self.setStyleSheet(STYLE)

        self._current_state = "IDLE"

        self.poller = Poller()
        self.poller.state_changed.connect(self.on_state_changed)
        self.poller.start()

    def on_state_changed(self, state, order):
        if state == self._current_state and state != "ARRIVED":
            if state == "MOVING" and order:
                self.moving_page.set_order(order)
            return

        self._current_state = state

        if state == "IDLE":
            self.stack.setCurrentIndex(0)
        elif state == "MOVING":
            self.moving_page.set_order(order)
            self.stack.setCurrentIndex(1)
        elif state == "ARRIVED":
            self.arrived_page.set_order(order)
            self.stack.setCurrentIndex(2)

    def on_direct_purchase(self, product_name):
        # 추후 아두이노 배출 신호 연동
        print(f"[직접구매] {product_name}")
        self.idle_page.show_toast(f"{product_name} 배출 중...")

    def keyPressEvent(self, e):
        if e.key() == Qt.Key_Escape:
            self.close()


if __name__ == "__main__":
    app = QApplication(sys.argv)
    app.setFont(QFont("Nanum Gothic", 12))
    win = MainWindow()
    sys.exit(app.exec_())

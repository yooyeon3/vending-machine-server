import sys
import io
import threading
import time
import requests
import qrcode
from datetime import datetime

from PyQt5.QtWidgets import (
    QApplication, QMainWindow, QWidget, QStackedWidget,
    QLabel, QPushButton, QVBoxLayout, QHBoxLayout, QFrame, QSizePolicy
)
from PyQt5.QtCore import Qt, QTimer, pyqtSignal, QObject, QSize
from PyQt5.QtGui import QFont, QPixmap, QImage, QPainter, QColor

SERVER  = "http://192.168.0.171:8080"
IMG_DIR = "/home/pi"

PRODUCTS = [
    {"name": "펩시 콜라",         "price": 1500, "img": f"{IMG_DIR}/pepsi.jpg",   "cmd_top": "drink1_top",    "cmd_bot": "drink1_bottom"},
    {"name": "레쓰비 마일드 커피", "price": 1200, "img": f"{IMG_DIR}/letsbe.jpg",  "cmd_top": "drink2_top",    "cmd_bot": "drink2_bottom"},
]

# ── 아두이노 시리얼 ────────────────────────────────────────
try:
    import serial
    arduino = serial.Serial('/dev/ttyUSB0', 9600, timeout=1)
    time.sleep(2)
    print("아두이노 연결됨")
except Exception as e:
    arduino = None
    print(f"아두이노 연결 실패: {e}")

def dispense(product, qty):
    if arduino is None:
        print(f"[시뮬] {product['name']} {qty}개 배출")
        return
    try:
        arduino.write((product["cmd_top"] + "\n").encode())
        if qty == 2:
            time.sleep(1)
            arduino.write((product["cmd_bot"] + "\n").encode())
        print(f"[배출] {product['name']} {qty}개")
    except Exception as e:
        print(f"배출 오류: {e}")

# ── 폴링 ──────────────────────────────────────────────────
class Poller(QObject):
    state_changed = pyqtSignal(str, object)

    def start(self):
        threading.Thread(target=self._loop, daemon=True).start()

    def _loop(self):
        while True:
            try:
                self._poll()
            except Exception as e:
                print("poll error:", e)
            time.sleep(3)

    def _poll(self):
        r = requests.get(f"{SERVER}/api/robot/delivery/status", timeout=5)
        data = r.json()

        if not data.get("active"):
            self.state_changed.emit("IDLE", None)
            return

        state = data.get("state", "MOVING")
        self.state_changed.emit(state, data)


# ── 공통 스타일 ────────────────────────────────────────────
def header_widget(text, bg="#3d2c1e"):
    w = QWidget()
    w.setFixedHeight(64)
    w.setStyleSheet(f"background:{bg};")
    l = QHBoxLayout(w)
    lbl = QLabel(text)
    lbl.setAlignment(Qt.AlignCenter)
    lbl.setStyleSheet(f"color:white; font-size:22px; font-weight:bold; background:transparent;")
    l.addWidget(lbl)
    return w

def rounded_pixmap(path, size):
    pix = QPixmap(path)
    if pix.isNull():
        pix = QPixmap(size, size)
        pix.fill(QColor("#e8dfd4"))
    pix = pix.scaled(size, size, Qt.KeepAspectRatioByExpanding, Qt.SmoothTransformation)
    # 중앙 크롭
    if pix.width() > size or pix.height() > size:
        x = (pix.width()  - size) // 2
        y = (pix.height() - size) // 2
        pix = pix.copy(x, y, size, size)
    # 둥근 마스크
    result = QPixmap(size, size)
    result.fill(Qt.transparent)
    painter = QPainter(result)
    painter.setRenderHint(QPainter.Antialiasing)
    painter.setBrush(painter.brush())
    from PyQt5.QtGui import QPainterPath
    path = QPainterPath()
    path.addRoundedRect(0, 0, size, size, 16, 16)
    painter.setClipPath(path)
    painter.drawPixmap(0, 0, pix)
    painter.end()
    return result


# ── 상품 카드 ──────────────────────────────────────────────
class ProductCard(QWidget):
    buy_requested = pyqtSignal(int, int)   # (product_idx, qty)

    def __init__(self, idx, product):
        super().__init__()
        self.idx = idx
        self.qty = 1
        self.setStyleSheet("background:white; border-radius:20px;")
        self.setFixedWidth(300)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(20, 24, 20, 24)
        layout.setSpacing(12)
        layout.setAlignment(Qt.AlignCenter)

        # 이미지
        img_lbl = QLabel()
        img_lbl.setAlignment(Qt.AlignCenter)
        img_lbl.setStyleSheet("background:transparent;")
        pix = rounded_pixmap(product["img"], 140)
        img_lbl.setPixmap(pix)
        layout.addWidget(img_lbl)

        # 상품명
        name_lbl = QLabel(product["name"])
        name_lbl.setAlignment(Qt.AlignCenter)
        name_lbl.setStyleSheet("font-size:17px; font-weight:bold; color:#3d2c1e; background:transparent;")
        layout.addWidget(name_lbl)

        # 가격
        price_lbl = QLabel(f"{product['price']:,}원")
        price_lbl.setAlignment(Qt.AlignCenter)
        price_lbl.setStyleSheet("font-size:20px; font-weight:bold; color:#c47d4a; background:transparent;")
        layout.addWidget(price_lbl)

        # 수량 선택
        qty_row = QHBoxLayout()
        qty_row.setSpacing(12)

        btn_minus = QPushButton("－")
        btn_minus.setFixedSize(40, 40)
        btn_minus.setStyleSheet(self._qty_btn_style())
        btn_minus.clicked.connect(self.decrease)

        self.qty_label = QLabel("1")
        self.qty_label.setFixedWidth(36)
        self.qty_label.setAlignment(Qt.AlignCenter)
        self.qty_label.setStyleSheet("font-size:20px; font-weight:bold; color:#3d2c1e; background:transparent;")

        btn_plus = QPushButton("＋")
        btn_plus.setFixedSize(40, 40)
        btn_plus.setStyleSheet(self._qty_btn_style())
        btn_plus.clicked.connect(self.increase)

        qty_row.addStretch()
        qty_row.addWidget(btn_minus)
        qty_row.addWidget(self.qty_label)
        qty_row.addWidget(btn_plus)
        qty_row.addStretch()
        layout.addLayout(qty_row)

        # 구매 버튼
        buy_btn = QPushButton("구매")
        buy_btn.setFixedHeight(48)
        buy_btn.setStyleSheet("""
            QPushButton {
                background:#3d2c1e; color:white; border-radius:14px;
                font-size:16px; font-weight:bold;
            }
            QPushButton:hover   { background:#c47d4a; }
            QPushButton:pressed { background:#2a1e14; }
        """)
        buy_btn.clicked.connect(lambda: self.buy_requested.emit(self.idx, self.qty))
        layout.addWidget(buy_btn)

    def _qty_btn_style(self):
        return """
            QPushButton {
                background:#f2ede4; color:#3d2c1e; border-radius:10px;
                font-size:18px; font-weight:bold; border:none;
            }
            QPushButton:hover   { background:#e8dfd4; }
            QPushButton:pressed { background:#d4c9ba; }
        """

    def decrease(self):
        if self.qty > 1:
            self.qty -= 1
            self.qty_label.setText(str(self.qty))

    def increase(self):
        if self.qty < 2:
            self.qty += 1
            self.qty_label.setText(str(self.qty))


# ── IDLE 화면 ──────────────────────────────────────────────
class IdlePage(QWidget):
    purchase_done = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)
        self.setStyleSheet("background:#faf8f4;")

        # 로고 포함 헤더
        header = QWidget()
        header.setFixedHeight(64)
        header.setStyleSheet("background:#3d2c1e;")
        hl = QHBoxLayout(header)
        hl.setContentsMargins(16, 0, 16, 0)

        logo = QLabel()
        logo.setStyleSheet("background:transparent;")
        logo_pix = QPixmap(f"{IMG_DIR}/pimto.png")
        if not logo_pix.isNull():
            logo.setPixmap(logo_pix.scaled(40, 40, Qt.KeepAspectRatio, Qt.SmoothTransformation))

        title = QLabel("PIMTO 자판기")
        title.setAlignment(Qt.AlignCenter)
        title.setStyleSheet("color:white; font-size:22px; font-weight:bold; background:transparent;")

        hl.addWidget(logo)
        hl.addWidget(title, stretch=1)
        hl.addSpacing(40)  # 로고 너비만큼 오른쪽 여백으로 중앙 맞춤

        layout.addWidget(header)

        body = QWidget()
        bl = QVBoxLayout(body)
        bl.setContentsMargins(40, 30, 40, 20)
        bl.setSpacing(0)

        cards_row = QHBoxLayout()
        cards_row.setSpacing(40)
        cards_row.setAlignment(Qt.AlignCenter)

        for i, p in enumerate(PRODUCTS):
            card = ProductCard(i, p)
            card.buy_requested.connect(self.on_buy)
            cards_row.addWidget(card)

        bl.addStretch()
        bl.addLayout(cards_row)
        bl.addStretch()

        guide = QLabel("앱/웹에서도 주문 가능합니다")
        guide.setAlignment(Qt.AlignCenter)
        guide.setStyleSheet("font-size:13px; color:#9e8c7a; background:transparent;")
        bl.addWidget(guide)

        layout.addWidget(body, stretch=1)

        # 토스트
        self.toast = QLabel("", self)
        self.toast.setAlignment(Qt.AlignCenter)
        self.toast.setStyleSheet("background:#3d2c1e; color:white; border-radius:12px; padding:12px 24px; font-size:15px; font-weight:bold;")
        self.toast.hide()
        self._timer = QTimer()
        self._timer.setSingleShot(True)
        self._timer.timeout.connect(self.toast.hide)

    def on_buy(self, idx, qty):
        product = PRODUCTS[idx]
        threading.Thread(target=dispense, args=(product, qty), daemon=True).start()
        self.show_toast(f"{product['name']} {qty}개 배출 중...")
        self.purchase_done.emit(product["name"])

    def show_toast(self, msg):
        self.toast.setText(msg)
        self.toast.adjustSize()
        self.toast.move((self.width() - self.toast.width()) // 2, self.height() - 70)
        self.toast.show()
        self._timer.start(2500)

    def resizeEvent(self, e):
        if self.toast.isVisible():
            self.toast.move((self.width() - self.toast.width()) // 2, self.height() - 70)


# ── MOVING 화면 ────────────────────────────────────────────
class MovingPage(QWidget):
    def __init__(self):
        super().__init__()
        self.setStyleSheet("background:#faf8f4;")
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        layout.addWidget(header_widget("배달 중...", "#c47d4a"))

        body = QWidget()
        bl = QVBoxLayout(body)
        bl.setContentsMargins(40, 30, 40, 20)
        bl.setSpacing(16)

        map_ph = QWidget()
        map_ph.setStyleSheet("background:#f2ede4; border-radius:16px; border:2px solid #e8dfd4;")
        ml = QVBoxLayout(map_ph)
        map_lbl = QLabel("로봇 이동 중\n\n(지도 연동 준비 중)")
        map_lbl.setAlignment(Qt.AlignCenter)
        map_lbl.setStyleSheet("color:#9e8c7a; font-size:18px; background:transparent; border:none;")
        ml.addWidget(map_lbl)
        bl.addWidget(map_ph, stretch=1)

        self.product_lbl = QLabel("")
        self.product_lbl.setAlignment(Qt.AlignCenter)
        self.product_lbl.setStyleSheet("font-size:16px; color:#c47d4a; font-weight:bold; background:transparent;")
        bl.addWidget(self.product_lbl)

        layout.addWidget(body, stretch=1)

    def set_order(self, data):
        if data:
            eta = data.get("eta", "")
            name = data.get("productName", "")
            self.product_lbl.setText(f"{name}  ·  {eta}" if eta else name)


# ── ARRIVED 화면 ───────────────────────────────────────────
class ArrivedPage(QWidget):
    def __init__(self):
        super().__init__()
        self.setStyleSheet("background:#faf8f4;")
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        layout.addWidget(header_widget("로봇이 도착했습니다!", "#22c55e"))

        body = QWidget()
        bl = QHBoxLayout(body)
        bl.setContentsMargins(60, 40, 60, 30)
        bl.setSpacing(60)
        bl.setAlignment(Qt.AlignCenter)

        # QR
        self.qr_lbl = QLabel()
        self.qr_lbl.setFixedSize(200, 200)
        self.qr_lbl.setAlignment(Qt.AlignCenter)
        self.qr_lbl.setStyleSheet("background:white; border:2px solid #e8dfd4; border-radius:14px;")
        bl.addWidget(self.qr_lbl, alignment=Qt.AlignVCenter)

        # PIN
        pin_box = QWidget()
        pin_box.setStyleSheet("background:white; border-radius:20px; border:2px solid #e8dfd4;")
        pl = QVBoxLayout(pin_box)
        pl.setContentsMargins(30, 30, 30, 30)
        pl.setSpacing(10)
        pl.setAlignment(Qt.AlignCenter)

        pin_title = QLabel("PIN 번호")
        pin_title.setAlignment(Qt.AlignCenter)
        pin_title.setStyleSheet("font-size:14px; color:#9e8c7a; background:transparent; border:none;")

        self.pin_val = QLabel("----")
        self.pin_val.setAlignment(Qt.AlignCenter)
        self.pin_val.setStyleSheet("font-size:40px; font-weight:bold; color:#3d2c1e; letter-spacing:4px; background:transparent; border:none;")

        pin_hint = QLabel("웹/앱에서 입력해주세요")
        pin_hint.setAlignment(Qt.AlignCenter)
        pin_hint.setStyleSheet("font-size:13px; color:#9e8c7a; background:transparent; border:none;")

        self.prod_lbl = QLabel("")
        self.prod_lbl.setAlignment(Qt.AlignCenter)
        self.prod_lbl.setStyleSheet("font-size:16px; color:#c47d4a; font-weight:bold; background:transparent; border:none;")

        pl.addWidget(pin_title)
        pl.addWidget(self.pin_val)
        pl.addWidget(pin_hint)
        pl.addSpacing(12)
        pl.addWidget(self.prod_lbl)

        bl.addWidget(pin_box, alignment=Qt.AlignVCenter)
        layout.addWidget(body, stretch=1)

    def set_order(self, data):
        if not data:
            return
        pin = data.get("pinCode", "----")
        self.pin_val.setText(pin)
        self.prod_lbl.setText(data.get("productName", ""))
        self._make_qr(pin)

    def _make_qr(self, pin):
        try:
            qr = qrcode.QRCode(box_size=6, border=2)
            qr.add_data(pin)
            qr.make(fit=True)
            img = qr.make_image(fill_color="black", back_color="white")
            buf = io.BytesIO()
            img.save(buf, format="PNG")
            buf.seek(0)
            qimg = QImage.fromData(buf.getvalue())
            pix = QPixmap.fromImage(qimg).scaled(196, 196, Qt.KeepAspectRatio, Qt.SmoothTransformation)
            self.qr_lbl.setPixmap(pix)
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

        self.stack.addWidget(self.idle_page)
        self.stack.addWidget(self.moving_page)
        self.stack.addWidget(self.arrived_page)

        self._state = "IDLE"

        self.poller = Poller()
        self.poller.state_changed.connect(self.on_state)
        self.poller.start()

    def on_state(self, state, order):
        if state == self._state and state == "MOVING":
            self.moving_page.set_order(order)
            return
        self._state = state
        if state == "IDLE":
            self.stack.setCurrentIndex(0)
        elif state == "MOVING":
            self.moving_page.set_order(order)
            self.stack.setCurrentIndex(1)
        elif state == "ARRIVED":
            self.arrived_page.set_order(order)
            self.stack.setCurrentIndex(2)

    def keyPressEvent(self, e):
        if e.key() == Qt.Key_Escape:
            self.close()


if __name__ == "__main__":
    app = QApplication(sys.argv)
    app.setFont(QFont("Nanum Gothic", 12))
    win = MainWindow()
    sys.exit(app.exec_())

# Changelog

## Unreleased

- Prepare the public FlintsLabs repository with MIT licensing, illustrated Thai/English usage guides, build instructions and GitHub Actions checks.
- Make the IP settings instrumentation test wait for the save acknowledgment before checking persisted state.
- Write release checksum entries with portable filenames so downloaded assets can be verified together.

## 1.2.1 — 2026-10-15

- แก้ Print Test บน RS38 / Android 13 ที่เริ่มด้วยกระดาษ Letter ชั่วคราวแล้วตัวอย่างว่างและไม่มีปุ่มพิมพ์หลังเลือก TSC
- สร้างตัวอย่างแรกตามขนาดที่เลือกในแอป แล้วสร้างใหม่ตามกระดาษ TSC ในหน้าต่าง Print พร้อมคงการตรวจขนาดก่อนส่งงานจริง
- เพิ่ม regression test เปิด Android Print ด้วย Letter และตรวจว่ามีตัวอย่าง 1 หน้าโดยไม่ส่งพิมพ์

## 1.2.0 — 2026-10-14

- เพิ่ม preset 148 × 210 มม. (A5) พร้อมขนาดเดิม 102 × 76 มม.
- เพิ่มขนาดเองในตั้งค่า กว้าง 20–168 มม. สูง 20–500 มม. ทศนิยม 1 ตำแหน่ง บันทึกได้ 20 ขนาด และเลือกใช้ร่วมกันบนหน้า Print / Print Test
- แสดงขนาดทั้งหมดใน Paper size ของ Android และสร้าง SIZE/BITMAP ตามขนาดที่เลือกในแต่ละงาน
- แยกความกว้างหัวพิมพ์กับความสูงแนวป้อนจากแนวตั้ง/แนวนอนของเอกสาร เพื่อคงขนาดจริง 148 × 210 มม.
- Print Test สร้างกรอบและเส้นวัดใหม่เมื่อเปลี่ยนขนาดในหน้าต่าง Print เส้นวัดเป็น 80 มม. หรือย่อลงพร้อมระบุความยาวสำหรับกระดาษแคบ
- ภาพตัวอย่างย่อให้เห็นเต็มแผ่นตามสัดส่วน รวมกระดาษแนวตั้งและหน้าจอกว้าง
- เก็บ IP และการตั้งค่าเดิมเมื่ออัปเดต ไม่มีคำสั่งเปลี่ยน Shift / Reference / gap sensor

## 1.1.0 — 2026-10-14

- ออกแบบใหม่เป็น 3 หน้า: พิมพ์, Print Test และตั้งค่า พร้อมธีมสว่าง/มืดและเมนูด้านข้างสำหรับจอกว้าง
- เพิ่มภาพตัวอย่างและฉลากทดสอบ 1 หน้า: กรอบห่างขอบ 2 มม., เส้นวัด 80 มม., QR `TSC-TEST-001` และเลข 4 สีขาวบนพื้นดำ
- ปุ่มทดสอบเปิดหน้าต่าง Print ของ Android ให้ตรวจเครื่องพิมพ์และจำนวนสำเนาก่อนยืนยัน ส่งผ่าน PrintService เดิม
- แยกการบันทึก IP กับการตรวจการเชื่อมต่อ แสดงข้อผิดพลาดและขั้นตอนเปิดบริการ/สิทธิ์ให้ชัดเจน
- คง IP ที่บันทึกไว้ พอร์ต 9100 ฉลาก 102×76 มม. 203 DPI และไม่เปลี่ยน Shift/Reference ที่เครื่อง

## 1.0.0 — 2026-10-14

- Android PrintService สำหรับ TSC MH261T ส่ง PDF เป็น TSPL BITMAP ผ่าน TCP โดยไม่ใช้ Mac
- รองรับ Android13+, เลือกหน้า/จำนวนสำเนา, fit/rotate, การยกเลิกและ timeout

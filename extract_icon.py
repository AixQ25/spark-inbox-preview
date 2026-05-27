"""
从设计图中裁剪 B 图标（中间三分之一），并生成 Android mipmap 资源
"""
from PIL import Image
import os

# === 配置 ===
src_path = r"F:\vibe coding\apps\spark-inbox-preview\design\icon.png"
res_base = r"F:\vibe coding\apps\spark-inbox-preview\native\app\src\main\res"

# Android mipmap 密度对应的尺寸 (px)
DENSITIES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

# === 1. 打开源图，获取尺寸 ===
img = Image.open(src_path)
w, h = img.size
print(f"源图尺寸: {w} x {h}")

# === 2. 裁剪中间三分之一（B图标） ===
# 水平方向大约 33%~67%，垂直方向尽量取完整高度（但设计图中图标有上下留白，取中间正方形区域更合适）
# 图标是正方形的，以宽度中心为基准取正方形裁切
center_x = w // 2
crop_w = w // 3  # 宽度的三分之一
# 从设计图来看，图标在垂直方向也居中，取与 crop_w 等高的正方形区域
crop_h = crop_w

left = center_x - crop_w // 2
top = (h - crop_h) // 2
right = center_x + crop_w // 2
bottom = (h + crop_h) // 2

# 边界安全检查
left = max(0, left)
top = max(0, top)
right = min(w, right)
bottom = min(h, bottom)

print(f"裁剪区域: left={left}, top={top}, right={right}, bottom={bottom}")
icon = img.crop((left, top, right, bottom))
print(f"裁剪后尺寸: {icon.size}")

# === 3. 保存裁剪预览到 drawable ===
drawable_dir = os.path.join(res_base, "drawable")
os.makedirs(drawable_dir, exist_ok=True)
preview_path = os.path.join(drawable_dir, "ic_launcher_preview.png")
icon.save(preview_path, "PNG")
print(f"预览图保存: {preview_path}")

# === 4. 生成各密度 mipmap ===
for density, size in DENSITIES.items():
    mipmap_dir = os.path.join(res_base, density)
    os.makedirs(mipmap_dir, exist_ok=True)

    resized = icon.resize((size, size), Image.LANCZOS)
    out_path = os.path.join(mipmap_dir, "ic_launcher.png")
    resized.save(out_path, "PNG")
    print(f"  {density}: {size}x{size} -> {out_path}")

print("\n全部完成！")

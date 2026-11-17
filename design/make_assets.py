from PIL import Image, ImageDraw
import os

src = r"C:\Users\RnLamsuly\.dsh\attachments\v1\objects\94\940092ae4c4cbe6dbebb4a78002c0e5a32c15700b711475cfc6a9a6d6bcdb846"
im = Image.open(src).convert("RGB")

root = r"D:\RanaSeek"
res = os.path.join(root, "app", "src", "main", "res")
drawable = os.path.join(res, "drawable-nodpi")
os.makedirs(drawable, exist_ok=True)
os.makedirs(os.path.join(root, "design"), exist_ok=True)

# 1) 头像（头肩特写）：稍向下一档，保证下巴与手完整
av = im.crop((185, 165, 645, 625)).resize((512, 512), Image.LANCZOS)
av.save(os.path.join(drawable, "avatar.png"))
av.save(os.path.join(root, "design", "avatar.png"))

# 2) 应用图标主体（人物主体，稍放大）
ic = im.crop((140, 130, 980, 970)).resize((1024, 1024), Image.LANCZOS)
ic.save(os.path.join(root, "design", "app_icon.png"))

# 3) 自适应图标前景：内容缩放到安全区（~65%），透明画布
fg = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
inner = ic.resize((660, 660), Image.LANCZOS).convert("RGBA")
fg.alpha_composite(inner, ((1024 - 660) // 2, (1024 - 660) // 2))
fg.save(os.path.join(drawable, "ic_launcher_foreground.png"))

# 4) 传统 mipmap（Android 7 及以下兜底）
densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for name, px in densities.items():
    d = os.path.join(res, f"mipmap-{name}")
    os.makedirs(d, exist_ok=True)
    ic.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher.png"))

# 5) 取图片边缘主色作为自适应图标背景
edge = []
for x in range(0, 1334, 12):
    edge.append(im.getpixel((x, 20)))
    edge.append(im.getpixel((x, 980)))
for y in range(0, 1002, 12):
    edge.append(im.getpixel((20, y)))
    edge.append(im.getpixel((1310, y)))
avg = tuple(sum(c[i] for c in edge) // len(edge) for i in range(3))
print("edge avg color:", "#%02X%02X%02X" % avg)
with open(os.path.join(root, "design", "launcher_bg.txt"), "w", encoding="utf-8") as f:
    f.write("#%02X%02X%02X" % avg)

# 预览：圆形头像 + 方图标 + 自适应图标效果（圆形遮罩模拟桌面）
prev = Image.new("RGB", (1260, 560), (15, 15, 15))
prev.paste(av.resize((470, 470), Image.LANCZOS), (30, 45))
prev.paste(ic.resize((470, 470), Image.LANCZOS), (540, 45))

icon = ic.resize((470, 470), Image.LANCZOS)
circle_prev = Image.new("RGBA", (470, 470), (0, 0, 0, 0))
mask = Image.new("L", (470, 470), 0)
ImageDraw.Draw(mask).ellipse((0, 0, 469, 469), fill=255)
circle_prev.paste(icon, (0, 0))
circle_prev.putalpha(mask)
bg = Image.new("RGBA", (470, 470), (15, 15, 15, 255))
bg.alpha_composite(circle_prev)
prev.paste(bg.convert("RGB"), (900 - 170, 45))
prev.save(os.path.join(root, "design", "preview_compare.png"))
print("done")

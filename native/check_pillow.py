from PIL import Image
print('Pillow OK')
img = Image.open(r'f:\vibe coding\apps\spark-inbox-preview\design\icon.png')
print(f'Image size: {img.size}')
print(f'Mode: {img.mode}')

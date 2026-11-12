import os
import cv2
import numpy as np
from PIL import Image, ImageDraw

def main():
    src_path = 'C:/Users/la/Downloads/AudioBooks-icon-eyes-higher.jpg'
    if not os.path.exists(src_path):
        raise FileNotFoundError(f'{src_path} not found')

    img = Image.open(src_path)
    # Squircle tile coordinates
    ymin, ymax = 531, 2521
    xmin, xmax = 1739, 3730
    tile = img.crop((xmin, ymin, xmax, ymax))
    tile_w, tile_h = tile.size # 1991 x 1990

    # 1. Background color
    # Base dark tile color
    bg_color_hex = "#18191C"
    
    # 2. Extract emblem for vector foreground
    # Convert tile to grayscale
    gray = cv2.cvtColor(np.array(tile), cv2.COLOR_RGB2GRAY)
    _, thresh = cv2.threshold(gray, 105, 255, cv2.THRESH_BINARY)
    
    # Use CCOMP hierarchy
    contours, hierarchy = cv2.findContours(thresh, cv2.RETR_CCOMP, cv2.CHAIN_APPROX_TC89_KCOS)
    
    # Scale and center into 108dp x 108dp canvas with safe zone fitting
    scale = 0.82 * (108.0 / 1990.0)
    center_src = 995.0
    center_dst = 54.0

    def transform_pt(pt):
        x = center_dst + (pt[0] - center_src) * scale
        y = center_dst + (pt[1] - center_src) * scale
        return round(float(x), 2), round(float(y), 2)

    # Filter out tile edges (near boundaries)
    valid_indices = []
    for i, c in enumerate(contours):
        x, y, w, h = cv2.boundingRect(c)
        if x <= 5 or y <= 5 or (x + w) >= 1985 or (y + h) >= 1985:
            continue
        if cv2.contourArea(c) > 500:
            valid_indices.append(i)

    # Build path strings
    # Sort into outer and inner holes based on hierarchy
    path_cmds = []
    for idx in valid_indices:
        c = contours[idx]
        hier = hierarchy[0][idx]
        approx = cv2.approxPolyDP(c, 2.0, True)
        pts = [transform_pt(p[0]) for p in approx]
        if len(pts) < 3:
            continue
        cmd = f"M{pts[0][0]},{pts[0][1]}"
        for p in pts[1:]:
            cmd += f" L{p[0]},{p[1]}"
        cmd += " Z"
        path_cmds.append(cmd)

    # Combined path for foreground
    full_path_data = " ".join(path_cmds)
    
    vector_xml = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#E2E4E9"
        android:fillType="evenOdd"
        android:pathData="{full_path_data}" />
</vector>
"""
    with open('app/src/main/res/drawable/ic_launcher_foreground.xml', 'w', encoding='utf-8') as f:
        f.write(vector_xml)
    print("Wrote app/src/main/res/drawable/ic_launcher_foreground.xml")

    # Update background
    with open('app/src/main/res/values/ic_launcher_background.xml', 'w', encoding='utf-8') as f:
        f.write(f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">{bg_color_hex}</color>
</resources>
""")
    print("Wrote app/src/main/res/values/ic_launcher_background.xml")

    with open('app/src/main/res/drawable/ic_launcher_background.xml', 'w', encoding='utf-8') as f:
        f.write(f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{bg_color_hex}"
        android:pathData="M0,0h108v108h-108z" />
</vector>
""")
    print("Wrote app/src/main/res/drawable/ic_launcher_background.xml")

    # 3. Generate raster mipmaps
    densities = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192,
    }

    # Prepare squircle mask for the tile
    # Tile aspect: high-res 1991 x 1990
    highres_tile = tile.resize((1024, 1024), Image.Resampling.LANCZOS)
    
    # Squircle with smooth rounded corners: corner radius ~ 220px on 1024px canvas
    squircle_mask = Image.new('L', (1024, 1024), 0)
    draw_s = ImageDraw.Draw(squircle_mask)
    draw_s.rounded_rectangle((0, 0, 1024, 1024), radius=230, fill=255)
    
    squircle_img = highres_tile.copy().convert('RGBA')
    squircle_img.putalpha(squircle_mask)

    # Circle mask for ic_launcher_round
    # Inside the circle, scale the emblem slightly so it fits beautifully inside the circular boundary
    circle_canvas = Image.new('RGBA', (1024, 1024), (24, 25, 28, 255))
    # Place scaled tile or emblem in center
    scaled_emblem = highres_tile.resize((920, 920), Image.Resampling.LANCZOS)
    circle_canvas.paste(scaled_emblem, (52, 52))
    circle_mask = Image.new('L', (1024, 1024), 0)
    draw_c = ImageDraw.Draw(circle_mask)
    draw_c.ellipse((0, 0, 1024, 1024), fill=255)
    circle_canvas.putalpha(circle_mask)

    for folder, size in densities.items():
        out_dir = os.path.join('app/src/main/res', folder)
        os.makedirs(out_dir, exist_ok=True)
        
        # ic_launcher.png (squircle)
        sq_out = squircle_img.resize((size, size), Image.Resampling.LANCZOS)
        sq_path = os.path.join(out_dir, 'ic_launcher.png')
        sq_out.save(sq_path, 'PNG')

        # ic_launcher_round.png (circle)
        rnd_out = circle_canvas.resize((size, size), Image.Resampling.LANCZOS)
        rnd_path = os.path.join(out_dir, 'ic_launcher_round.png')
        rnd_out.save(rnd_path, 'PNG')

        print(f"Generated {sq_path} and {rnd_path} at {size}x{size}")

    # Also update app/src/main/res/drawable/ic_launcher.jpg (legacy square fallback)
    fallback_jpg = highres_tile.resize((512, 512), Image.Resampling.LANCZOS)
    fallback_jpg.save('app/src/main/res/drawable/ic_launcher.jpg', 'JPEG', quality=95)
    print("Updated app/src/main/res/drawable/ic_launcher.jpg")

if __name__ == '__main__':
    main()

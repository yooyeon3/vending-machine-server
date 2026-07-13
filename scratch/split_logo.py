import os
import sys

# Ensure Pillow is installed
try:
    from PIL import Image, ImageChops
except ImportError:
    import subprocess
    print("Installing Pillow...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
    from PIL import Image, ImageChops

def trim(im):
    # Trim white/transparent margins to maximize the logo size
    bg = Image.new(im.mode, im.size, (255, 255, 255))
    diff = ImageChops.difference(im, bg)
    diff = ImageChops.add(diff, diff, 2.0, -100)
    bbox = diff.getbbox()
    if bbox:
        return im.crop(bbox)
    return im

def main():
    img_path = r"src/main/resources/static/images/pimto.png"
    if not os.path.exists(img_path):
        print(f"Error: {img_path} not found.")
        sys.exit(1)
        
    img = Image.open(img_path)
    
    # Handle alpha channel
    if img.mode in ('RGBA', 'LA') or (img.mode == 'P' and 'transparency' in img.info):
        background = Image.new("RGBA", img.size, (255, 255, 255, 255))
        background.paste(img, (0, 0), img.convert("RGBA"))
        img = background.convert("RGB")
    else:
        img = img.convert("RGB")
        
    # Trim white borders to get the exact bounding box of the logo
    trimmed_img = trim(img)
    
    # 2x2 grid of A4 sheets at 300 DPI:
    # Width = 2 * 2480 = 4960
    # Height = 2 * 3508 = 7016
    target_grid_w = 4960
    target_grid_h = 7016
    
    scale_w = target_grid_w / trimmed_img.width
    scale_h = target_grid_h / trimmed_img.height
    scale = min(scale_w, scale_h)
    
    new_w = int(trimmed_img.width * scale)
    new_h = int(trimmed_img.height * scale)
    
    resized_logo = trimmed_img.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    # Create the giant 2x2 grid canvas (4960 x 7016) with white background
    giant_canvas = Image.new("RGB", (target_grid_w, target_grid_h), (255, 255, 255))
    
    # Center the resized logo on the giant canvas
    offset_x = (target_grid_w - new_w) // 2
    offset_y = (target_grid_h - new_h) // 2
    giant_canvas.paste(resized_logo, (offset_x, offset_y))
    
    # Now split the giant canvas into 4 quadrants
    a4_w = 2480
    a4_h = 3508
    
    quadrants = {
        "NW": (0, 0, a4_w, a4_h),
        "NE": (a4_w, 0, target_grid_w, a4_h),
        "SW": (0, a4_h, a4_w, target_grid_h),
        "SE": (a4_w, a4_h, target_grid_w, target_grid_h)
    }
    
    output_dir = "print_quadrants"
    os.makedirs(output_dir, exist_ok=True)
    
    names = {
        "NW": "1_top_left.png",
        "NE": "2_top_right.png",
        "SW": "3_bottom_left.png",
        "SE": "4_bottom_right.png"
    }
    
    for quad_name, box in quadrants.items():
        cropped = giant_canvas.crop(box)
        output_path = os.path.join(output_dir, names[quad_name])
        cropped.save(output_path, "PNG")
        print(f"Saved {quad_name} to {output_path}")
        
    print("SUCCESS")

if __name__ == "__main__":
    main()

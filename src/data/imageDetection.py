import os

import cv2
import numpy as np
import tkinter as tk

root = tk.Tk()
SCREEN_WIDTH = root.winfo_screenwidth()
SCREEN_HEIGHT = root.winfo_screenheight()

def display_fullscreen_no_stretch(img, screen_width, screen_height):
    img_height, img_width = img.shape[:2]

    # 2. Calculate new dimensions while maintaining aspect ratio
    # Calculate scale factors for both dimensions
    scale_width = screen_width / img_width
    scale_height = screen_height / img_height
    
    # Use the minimum scale factor to fit the image entirely within the screen
    scale = min(scale_width, scale_height)
    
    # Calculate new dimensions
    new_width = int(img_width * scale)
    new_height = int(img_height * scale)
    
    # 3. Resize the image
    # INTER_AREA is good for shrinking; INTER_CUBIC or INTER_LINEAR for enlarging
    resized_img = cv2.resize(img, (new_width, new_height), interpolation=cv2.INTER_AREA)

    # 4. (Optional) Pad the image with black borders to fill the whole screen
    # Calculate padding for top/bottom or left/right
    top_pad = (screen_height - new_height) // 2
    bottom_pad = screen_height - new_height - top_pad
    left_pad = (screen_width - new_width) // 2
    right_pad = screen_width - new_width - left_pad
    
    padded_img = cv2.copyMakeBorder(resized_img, top_pad, bottom_pad, left_pad, right_pad, cv2.BORDER_CONSTANT, value=[0, 0, 0])

    return padded_img

    # 5. Display the image in a fullscreen window
    window_name = "Fullscreen Image"
    cv2.namedWindow(window_name, cv2.WINDOW_NORMAL) # Create resizable window
    cv2.setWindowProperty(window_name, cv2.WND_PROP_FULLSCREEN, cv2.WINDOW_FULLSCREEN) # Set to fullscreen
    cv2.imshow(window_name, padded_img)
    
    print("Press any key to exit.")
    cv2.waitKey(0) # Wait indefinitely for a key press
    cv2.destroyAllWindows()

def nothing(x):
    pass

def detectRectangles(
    fp, # can be .png or .mp4
    min_area = 0,
    max_area = 1e+7,
    blur_size = 5,
    epsilon_scalar = 0.02,
    canny_lower_threshold = 30,
    canny_upper_threshold = 50,
    use_adaptive_threshold = False,
    adaptive_threshold_block_size = 11,
    adaptive_threshold_constant = 2.0,
    window_name = "window",
):
    is_video = os.path.splitext(fp)[-1] == ".mp4"
    
    if is_video:
        cap = cv2.VideoCapture(fp)
    
    cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
    # Set the window to fullscreen mode
    cv2.setWindowProperty(window_name, cv2.WND_PROP_FULLSCREEN, cv2.WINDOW_FULLSCREEN)

    cv2.createTrackbar("Blur Size", window_name, blur_size, 32, nothing)
    # Trackbar doesn't allow float values. Need to scale this value back down later.
    cv2.createTrackbar("Epsilon", window_name, int(epsilon_scalar * 100), 100, nothing)
    
    if use_adaptive_threshold:
        cv2.createTrackbar("Block Size", window_name, adaptive_threshold_block_size, 255, nothing)
    else:
        cv2.createTrackbar("Threshold1", window_name, canny_lower_threshold, 255, nothing)
        cv2.createTrackbar("Threshold2", window_name, canny_upper_threshold, 255, nothing)

    while(True):
        blur_size = cv2.getTrackbarPos("Blur Size", window_name)
        epsilon_scalar = cv2.getTrackbarPos("Epsilon", window_name)

        if use_adaptive_threshold:
            adaptive_threshold_block_size = cv2.getTrackbarPos("Block Size", window_name)
            
            adaptive_threshold_block_size = max(3, adaptive_threshold_block_size)
            if adaptive_threshold_block_size % 2 == 0:
                adaptive_threshold_block_size += 1
        else:
            canny_lower_threshold = cv2.getTrackbarPos("Threshold1", window_name)
            canny_upper_threshold = cv2.getTrackbarPos("Threshold2", window_name)
        
        blur_size = max(1, blur_size)
        if blur_size % 2 == 0:
            blur_size += 1
        
        # Scale back to decimal range.
        epsilon_scalar = float(epsilon_scalar) / 100.0
    
        if is_video:
            ret, image = cap.read()
            if not ret:
                cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
                ret, image = cap.read()
        else:
            image = cv2.imread(fp)
    
        # Process Image
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        blurred = cv2.GaussianBlur(gray, (blur_size, blur_size), 0)
        
        if use_adaptive_threshold:
            tmp = cv2.adaptiveThreshold(
                blurred,
                255,
                cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                cv2.THRESH_BINARY_INV,
                adaptive_threshold_block_size,
                adaptive_threshold_constant,
            )
        else:
            tmp = cv2.Canny(
                image=blurred,
                threshold1=canny_lower_threshold,
                threshold2=canny_upper_threshold,
            )
        
        out_img = tmp
        if len(out_img.shape) == 2 or out_img.shape[2] == 1:
            out_img = cv2.cvtColor(out_img, cv2.COLOR_GRAY2BGR)

        # Find and filter contours
        contours, _ = cv2.findContours(tmp, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        for cnt in contours:
            # Filter out invalid sized contours.
            area = cv2.contourArea(cnt)
            if area < min_area or area > max_area:
                continue

            # Use Convex Hull to ignore rounded corners
            hull = cv2.convexHull(cnt)
            
            # Approximate the hull shape
            peri = cv2.arcLength(hull, True)
            approx = cv2.approxPolyDP(hull, epsilon_scalar * peri, True)
            
            # If 4 vertices are found, it's a candidate for a rounded rectangle
            if len(approx) == 4:
                x, y, w, h = cv2.boundingRect(cnt)
                # Draw rectangle for visualization.
                cv2.rectangle(out_img, (x, y), (x + w, y + h), (0, 255, 0), 2)
        
        res = display_fullscreen_no_stretch(out_img, SCREEN_WIDTH, SCREEN_HEIGHT)
        cv2.imshow(window_name, res)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
    
    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    fp = "./imageDetectionSample.mp4"
    detectRectangles(
        fp,
        min_area=200*900,
        max_area=300*1050,
        blur_size=5,
        epsilon_scalar=0.02,
        canny_lower_threshold=30,
        canny_upper_threshold=50,
        use_adaptive_threshold=False,
        adaptive_threshold_block_size=11,
        adaptive_threshold_constant=2.0,
    )

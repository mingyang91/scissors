import logging

import cv2
from skimage.feature import hog

logging.basicConfig()
logger = logging.getLogger("common")
logger.setLevel(logging.DEBUG)


def reduce(img):
    factor = 800
    height, width, _ = img.shape
    newHeight, newWidth = 600, 800
    if height > width:
        newHeight, newWidth = factor, width * factor / height
    else:
        newHeight, newWidth = height * factor / width, factor

    return cv2.resize(img, dsize=(int(newWidth), int(newHeight)), interpolation=cv2.INTER_AREA)


def compute_hog(file: str):
    img = cv2.imread(file)
    reduced = reduce(img)
    fd, hog_img = hog(reduced, orientations=8, pixels_per_cell=(16, 16), cells_per_block=(4, 4), block_norm='L2',
                      visualize=True)
    logger.debug(f"HOG computed: {file}")
    return fd

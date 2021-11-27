import logging
from typing import List

import cv2
import numpy.ma
from skimage.feature import hog

logging.basicConfig()
logger = logging.getLogger("common")
logger.setLevel(logging.DEBUG)


def reduce(img):
    factor = 800
    height, width, _ = img.shape
    newHeight, newWidth = 300, 400
    if height > width:
        newHeight, newWidth = factor, width * factor / height
    else:
        newHeight, newWidth = height * factor / width, factor

    return cv2.resize(img, dsize=(int(newWidth), int(newHeight)), interpolation=cv2.INTER_AREA)


def compute_hog(file: str, rotates: List[int] = [None]) -> object:
    handle = open(file, "rb")
    bytes = bytearray(handle.read())
    handle.close()
    nparr = numpy.ma.asarray(bytes, dtype=numpy.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_UNCHANGED)
    arr = []
    for rotate in rotates:
        if rotate is not None:
            img = cv2.rotate(img, rotate)
        img = reduce(img)
        fd, hog_img = hog(img, orientations=8, pixels_per_cell=(64, 64), cells_per_block=(4, 4), block_norm='L2',
                          visualize=True)
        degree = 90 * 0 if rotate is None else rotate
        logger.debug(f"HOG computed with {degree}°: {file}")
        arr.append(fd)
    return arr

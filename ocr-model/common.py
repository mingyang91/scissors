import logging
import random

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


def compute_hog(file: str, rotate: int = None) -> object:
    fd = open(file, "rb")
    bytes = bytearray(fd.read())
    nparr = numpy.ma.asarray(bytes, dtype=numpy.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_UNCHANGED)
    if rotate is not None:
        img = cv2.rotate(img, rotate)
    reduced = reduce(img)
    fd, hog_img = hog(reduced, orientations=8, pixels_per_cell=(16, 16), cells_per_block=(4, 4), block_norm='L2',
                      visualize=True)
    logger.debug(f"HOG computed: {file}")
    return fd

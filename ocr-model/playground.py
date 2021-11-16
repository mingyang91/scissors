import logging
from os import listdir
from os.path import join

import cv2
import numpy as np
from skimage.feature import hog
from sklearn import svm
from sklearn.metrics import classification_report, accuracy_score

logging.basicConfig()
logger = logging.getLogger("playground")
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


covers = [join(r"D:\sample\cover", name) for name in listdir(r"D:\sample\cover")]
contents = [join(r"D:\sample\content", name) for name in listdir(r"D:\sample\content")]

labels = ["cover" for _ in covers] + ["content" for _ in contents]
labels_nd = np.array(labels).reshape(len(labels), 1)

hog_features = [compute_hog(file) for file in covers] + [compute_hog(file) for file in contents]

data_frame = np.hstack((hog_features, labels_nd))

np.random.shuffle(data_frame)

percentage = 80
partition = int(len(hog_features) * percentage / 100)

clf = svm.SVC()

x_train, x_test = data_frame[:partition, :-1], data_frame[partition:, :-1]
y_train, y_test = data_frame[:partition, -1:].ravel(), data_frame[partition:, -1:].ravel()

clf.fit(x_train, y_train)

y_pred = clf.predict(x_test)

print("Accuracy: " + str(accuracy_score(y_test, y_pred)))
print('\n')
print(classification_report(y_test, y_pred))
# hog_img_rescaled = exposure.rescale_intensity(hog_img, in_range=(0, 10))

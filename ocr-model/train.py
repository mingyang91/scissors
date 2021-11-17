import datetime
import logging
import pickle
from os import listdir
from os.path import join

import cv2
import numpy as np
from sklearn import svm
from sklearn.metrics import classification_report, accuracy_score
from typing import Tuple, List

from common import compute_hog

logging.basicConfig()
logger = logging.getLogger("train")
logger.setLevel(logging.DEBUG)


rotates = [None, cv2.ROTATE_90_CLOCKWISE, cv2.ROTATE_180, cv2.ROTATE_90_COUNTERCLOCKWISE]


def train():

    cover_features = dataset("cover")
    content_features = dataset("content")
    hog_features = cover_features + content_features

    labels = ["cover" for _ in cover_features] + ["content" for _ in content_features]
    labels_nd = np.array(labels).reshape(len(labels), 1)
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

    pickle.dump(clf, open(fr".\models\{datetime.date.today().isoformat()}.svm.model", "wb"))


def dataset(label: str) -> List[np.array]:
    files = [join(fr"D:\sample\{label}", name) for name in listdir(fr"D:\sample\{label}")]

    hog_features = [compute_hog(file, rotate) for rotate in rotates for file in files]

    return hog_features

if __name__ == "__main__":
    train()

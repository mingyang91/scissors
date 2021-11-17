import datetime
import logging
import pickle
from os import listdir
from os.path import join

import numpy as np
from sklearn import svm
from sklearn.metrics import classification_report, accuracy_score
from common import compute_hog

logging.basicConfig()
logger = logging.getLogger("train")
logger.setLevel(logging.DEBUG)


def train():
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

    pickle.dump(clf, open(fr".\models\{datetime.date.today().isoformat()}.svm.model", "wb"))


if __name__ == "main":
    train()

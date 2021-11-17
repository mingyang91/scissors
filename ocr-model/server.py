from typing import List

from fastapi import FastAPI
from paddleocr import PaddleOCR
import pickle
from sklearn import svm
from train import compute_hog
import numpy as np

app = FastAPI()

paddle = PaddleOCR(use_angle_cls=True, lang='ch', use_gpu=False)

model: svm.SVC = pickle.load(open(r".\models\2021-11-17.svm.model", "rb"))


@app.get("/ocr")
async def ocr(path: str):
    result = paddle.ocr(path, cls=True)
    resp = list(map(extract_span, result))
    return resp


def extract_span(span: List[any]):
    area_list, text_tuple = span
    text, accuracy = text_tuple
    areas = list(map(to_coordinate, area_list))
    return {
        'areas': areas,
        'text': text,
        'accuracy': float(accuracy)
    }


def to_coordinate(position: List[float]):
    x, y = position
    return {'x': float(x), 'y': float(y)}


@app.get("/clf")
async def clf(path: str):
    hog = compute_hog(path)
    result = model.predict(np.array(hog).reshape(1, len(hog)))
    return result[0]


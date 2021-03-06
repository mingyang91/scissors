from typing import List

from fastapi import FastAPI
import pickle
from sklearn import svm
from train import compute_hog
import numpy as np
from paddleocr import PaddleOCR

app = FastAPI()

paddle = PaddleOCR(use_angle_cls=True, lang='ch', use_gpu=False)

model: svm.SVC = pickle.load(open(r".\models\2022-07-09.svm.model", "rb"))


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
    try:
        hog = compute_hog(path)[0]
        result = model.predict(np.array(hog).reshape(1, len(hog)))
        return {'type': result[0]}
    except BaseException as err:
        return {'type': 'error', 'message': str(err)}


@app.get("/health")
async def health():
    return "OK"

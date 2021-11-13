from typing import List

from fastapi import FastAPI
from paddleocr import PaddleOCR

app = FastAPI()

ocr = PaddleOCR(use_angle_cls=True, lang='ch', use_gpu=False)


@app.get("/")
async def hello(path: str):
    result = ocr.ocr(path, cls=True)
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

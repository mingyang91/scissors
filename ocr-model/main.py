import os
import threading
import psutil
import uvicorn


def set_interval(interval):
    def decorator(function):
        def wrapper(*args, **kwargs):
            stopped = threading.Event()

            def loop(): # executed in another thread
                while not stopped.wait(interval): # until stopped
                    function(*args, **kwargs)

            t = threading.Thread(target=loop)
            t.daemon = True # stop if the program exits
            t.start()
            return stopped
        return wrapper
    return decorator


@set_interval(1)
def check_parent_alive():
    if not psutil.pid_exists(os.getppid()):
        exit(0)


if __name__ == "__main__":
    stop_check = check_parent_alive()
    uvicorn.run("server:app", host="127.0.0.1", port=9000, log_level="info")
    stop_check.set()

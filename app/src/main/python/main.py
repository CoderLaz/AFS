import matplotlib.pyplot as plt
import numpy as np
from PIL import Image
import cv2
import io
import base64


def main(x, y, y1, y2, y3):
    x = [float(i) for i in list(filter(None, x.split('<br>')))]
    y = [float(i) for i in list(filter(None, y.split('<br>')))]
    y1 = [float(i) for i in list(filter(None, y1.split('<br>')))]
    y2 = [float(i) for i in list(filter(None, y2.split('<br>')))]
    y3 = [float(i) for i in list(filter(None, y3.split('<br>')))]

    # Plotting Q vs ro graph
    plot1 = plt.figure(1)
    plt.plot(x, y, marker='o')
    plt.xlabel("Radius of Cylinder 'ro' after insulation in m (Meters)")
    plt.ylabel("Heat Transfer 'Q' in W (watts)")
    plt.title('Graphical Representation of Q vs ro ', fontweight='bold', fontsize=14)
    plt.text(sum(x) / len(x) * 0.95, sum(y) / len(y) * 0.99, f'Critical Thickness: {x[y.index(max(y))]}',
             fontsize=11,
             bbox=dict(facecolor='navy', pad=8), fontweight='bold', color='white')

    # Plotting Resistances vs ro
    plot2 = plt.figure(2)
    plt.plot(x, y1, marker='o', label="Conductive Resistance")
    plt.plot(x, y2, marker='o', label="Convective Resistance", color='green')
    plt.plot(x, y3, marker='o', label="Total Resistance", color='purple')
    plt.xlabel("Radius of Cylinder 'ro' after insulation in m (Meters)")
    plt.ylabel("Resistances")
    plt.title('Graphical Representation of Resistances vs ro ', fontweight='bold', fontsize=14)
    plt.legend()
    # plt.show()

    plot1.canvas.draw()

    img1 = np.fromstring(plot1.canvas.tostring_rgb(), dtype=np.uint8, sep='')
    img1 = img1.reshape(plot1.canvas.get_width_height()[::-1] + (3,))
    img1 = cv2.cvtColor(img1, cv2.COLOR_RGB2BGR)

    pil_im1 = Image.fromarray(img1)
    buff1 = io.BytesIO()
    pil_im1.save(buff1, format="PNG")
    img1_str = base64.b64encode(buff1.getvalue())

    plot2.canvas.draw()

    img2 = np.fromstring(plot2.canvas.tostring_rgb(), dtype=np.uint8, sep='')
    img2 = img2.reshape(plot2.canvas.get_width_height()[::-1] + (3,))
    img2 = cv2.cvtColor(img2, cv2.COLOR_RGB2BGR)

    pil_im2 = Image.fromarray(img2)
    buff2 = io.BytesIO()
    pil_im2.save(buff2, format="PNG")
    img2_str = base64.b64encode(buff2.getvalue())

    byte_imgs = "" + str(img1_str, 'utf-8') + "&&division&&" + str(img2_str, 'utf-8')

    return byte_imgs


def clear():
    plt.clf()
    plt.clf()
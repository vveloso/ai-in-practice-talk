# %%

import tensorflow as tf
import matplotlib.pyplot as plt
import time

# %%

image = tf.keras.preprocessing.image.load_img(
    'busy-street.jpg', 
    target_size=(405, 540),
    interpolation='bilinear')

plt.subplot(1, 2, 1)
plt.imshow(image)

image_data = tf.keras.preprocessing.image.img_to_array(image)
image_data = image_data / 127.5 - 1.0
input = tf.expand_dims(image_data, axis=0)

# %%

interpreter = tf.lite.Interpreter(
    model_path='AnimeGANv2_Hayao-64.tflite')

interpreter.resize_tensor_input(0, input.shape, strict=True)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

interpreter.set_tensor(input_details[0]['index'], input)

# %%

start = time.time()

interpreter.invoke()

print ('Processing time was {} sec'.format(time.time()-start))

output_data = (interpreter.get_tensor(
    output_details[0]['index'])[0] + 1.) / 2 * 255
output_data = tf.cast(tf.clip_by_value(output_data, 0, 255), 
    tf.uint8)

# %%

tf.keras.preprocessing.image.save_img(
    'output-tflite.png', output_data)

plt.subplot(1, 2, 2)
plt.imshow(output_data)

plt.show()

# %%

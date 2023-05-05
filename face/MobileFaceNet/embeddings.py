import numpy as np
import tensorflow as tf

interpreter = tf.lite.Interpreter(model_path="mobilefacenet.tflite")
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

input_shape = input_details[0]['shape']
print("Input shape:", input_shape)

img = tf.keras.preprocessing.image.load_img('jurica-koletic-7YVZYZeITc8-unsplash.jpg',
    color_mode='rgb',
    target_size=(input_shape[1], input_shape[2]),
    interpolation='bilinear')
img = (tf.keras.preprocessing.image.img_to_array(img) - 127.5) / 128
img = tf.expand_dims(img, 0)
interpreter.set_tensor(input_details[0]['index'], img)

# Obtain the embeddings from said image.
interpreter.invoke()
embeddings = interpreter.get_tensor(output_details[0]['index'])

print("Embeddings shape:", embeddings.shape, "norm", np.linalg.norm(embeddings))
print("Embeddings:")
print(repr(embeddings))

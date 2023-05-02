import numpy as np
import tensorflow as tf

interpreter = tf.lite.Interpreter(model_path="mobilefacenet.tflite")
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Generate a random imput "image" and preprocess it before setting it as the model's input.
input_shape = input_details[0]['shape']
img = np.array(np.random.random_sample(input_shape), dtype=np.float32)
img = (np.float32(img) - 127.5) / 128
interpreter.set_tensor(input_details[0]['index'], img)

# Obtain the embeddings from said random image.
interpreter.invoke()
embeddings = interpreter.get_tensor(output_details[0]['index'])

print("Embeddings shape:", embeddings.shape, "norm", np.linalg.norm(embeddings))

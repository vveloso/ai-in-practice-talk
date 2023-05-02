# %%
import tensorflow as tf

interpreter = tf.lite.Interpreter(
    model_path='model/show_and_tell.tflite')

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# %%

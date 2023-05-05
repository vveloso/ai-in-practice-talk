# %%
import tensorflow as tf

model = tf.lite.TFLiteConverter.from_saved_model(
    'fashion-cnn'
).convert()

with open('converted-fashion.tflite', 'wb') as file:
    file.write(model)

# %%

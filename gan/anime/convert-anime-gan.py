import tensorflow as tf

converter = tf.compat.v1.lite.TFLiteConverter.from_saved_model(
    'pb_model_Hayao-64',
    signature_key = 'custom_signature')

tflite_model = converter.convert()

# Save the model.
with open('AnimeGANv2_Hayao-64.tflite', 'wb') as f:
  f.write(tflite_model)

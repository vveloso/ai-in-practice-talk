# %%
import tensorflow as tf
import matplotlib.pyplot as plt
import time

# %%

print('Loading image...')

image = tf.keras.preprocessing.image.load_img(
    'busy-street.jpg', 
    target_size=(540, 720),
    interpolation='bilinear')

plt.subplot(1, 2, 1)
plt.imshow(image)

image_data = tf.keras.preprocessing.image.img_to_array(image)
image_data = image_data / 127.5 - 1.0
input = tf.expand_dims(image_data, axis=0)

# %%

print('Loading model...')

loaded = tf.saved_model.load('pb_model_Hayao-64')
infer = loaded.signatures["custom_signature"]

# %%
print('Processing...')
start = time.time()

output = infer(input)['output']

print('Processing time was {} sec'.format(time.time()-start))

output_data = (output[0] + 1.) / 2 * 255
output_data = tf.cast(tf.clip_by_value(output_data, 0, 255), tf.uint8)

# %%

tf.keras.preprocessing.image.save_img('output.png', output_data)

plt.subplot(1, 2, 2)
plt.imshow(output_data)

plt.show()

# %%

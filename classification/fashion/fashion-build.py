# %%
import tensorflow as tf
import numpy as np
import drawing as draw

# %%
dataset = tf.keras.datasets.fashion_mnist
(train_images, train_labels), (test_images, test_labels) = \
    dataset.load_data()
del dataset

# %%

draw.show_image(train_images[60])

# %%

# Scale image values so each pixel value is in the [0, 1] interval.
train_images = train_images / 255.0
test_images = test_images / 255.0

draw.show_image(train_images[60])

# %%

classes = ['T-shirt/top', 'Trouser', 'Pullover', 'Dress', 'Coat',
           'Sandal', 'Shirt', 'Sneaker', 'Bag', 'Ankle boot']

# %%

# Training an improved model
# see "Fashion- MNIST with tf.Keras", Margaret Maynard-Reid, 2018

model = tf.keras.Sequential([
    tf.keras.layers.Conv2D(64, 2, padding='same', activation='relu'), # spatial convolution over images
    tf.keras.layers.MaxPool2D(2), # max pooling operation for 2D spatial data (downsampling).
    tf.keras.layers.Dropout(0.3), # randomly sets input units to 0 with a given frequency at each step during training time
    tf.keras.layers.Conv2D(32, 2, padding='same', activation='relu'),
    tf.keras.layers.MaxPool2D(2),
    tf.keras.layers.Dropout(0.3),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(256, activation='relu'),
    tf.keras.layers.Dropout(0.5),
    tf.keras.layers.Dense(10, activation='softmax')
])
model.compile(optimizer='adam',
              loss=tf.keras.losses.SparseCategoricalCrossentropy(),
              metrics=['accuracy'])

history = model.fit(tf.expand_dims(train_images, 3), train_labels, 
                    epochs=10, 
                    validation_split=0.1)

test_loss, test_acc = model.evaluate(tf.expand_dims(test_images, 3),  test_labels, verbose=2)

draw.plot_history(history)

#%%

# model.save('fashion-cnn')


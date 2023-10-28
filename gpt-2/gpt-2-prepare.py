# %%

import numpy as np
import keras_nlp
import tensorflow as tf
import tensorflow_datasets as tfds
import tensorflow_text as tf_text
from tensorflow import keras
from tensorflow.lite.python import interpreter
import time

# %%

gpt2_tokenizer = keras_nlp.models.GPT2Tokenizer.from_preset("gpt2_base_en")
gpt2_preprocessor = keras_nlp.models.GPT2CausalLMPreprocessor.from_preset(
    "gpt2_base_en",
    sequence_length=256,
    add_end_token=True,
)
gpt2_lm = keras_nlp.models.GPT2CausalLM.from_preset("gpt2_base_en", preprocessor=gpt2_preprocessor)

# %%

output = gpt2_lm.generate("My trip to Yosemite was", max_length=200)
print("\nGPT-2 output:")
print(output.numpy().decode("utf-8"))

# %%

output = gpt2_lm.generate("That Italian restaurant is", max_length=200)
print("\nGPT-2 output:")
print(output.numpy().decode("utf-8"))

# %%

cnn_ds = tfds.load('cnn_dailymail', as_supervised=True)

# %%

from nltk import tokenize
import nltk

nltk.download('punkt')

#%%

for article, highlights in cnn_ds['train']:
  combination = article + tf.constant(' TL;DR ') + tf.strings.regex_replace(highlights, "\n", " ")
  word_count=len(tokenize.word_tokenize(str(combination.numpy())))
  if word_count < 256:
    print(combination.numpy())
    print(article.numpy())
    print(highlights.numpy())
    break

# %%
import progressbar

short_texts = []
total = len(cnn_ds['train'])
progressbar_update_freq = 1000
count = 0

widgets = [' [',
         progressbar.Timer(format= 'elapsed time: %s'),
         '] ',
           progressbar.Bar('*'),' (',
           progressbar.ETA(), ') ',
          ]
bar = progressbar.ProgressBar(
    maxval=total // progressbar_update_freq + 2,
    widgets=widgets).start()

for article, highlights in cnn_ds['train']:
  combination = article + tf.constant(' TL;DR ') + tf.strings.regex_replace(highlights, "\n", " ")
  word_count = len(tokenize.word_tokenize(str(combination.numpy())))
  if word_count < 256:
    short_texts.append(combination)
  count += 1
  if count % progressbar_update_freq == 0:
    bar.update(count / progressbar_update_freq)

# %%

def save_texts(texts):
    np.savez('data/selected_texts.npz', texts)

def load_texts():
    restored_texts = list()
    with np.load('data/selected_texts.npz', allow_pickle=True) as data:
      for file in data.files:
        restored_texts.extend(data[file].tolist())
    return restored_texts

# %%

# Save the list of short combinations of articles and summaries (sort of a checkpoint).
save_texts(short_texts)

# %%

short_texts = load_texts()

tiny_texts = list()
for text in short_texts:
    if len(text.numpy()) < 512:
        tiny_texts.append(str(text.numpy()))


# %%

train_texts=tiny_texts

tf_train_ds = tf.data.Dataset.from_tensor_slices(train_texts)
processed_ds = tf_train_ds.map(gpt2_preprocessor, tf.data.AUTOTUNE).batch(20).cache().prefetch(tf.data.AUTOTUNE)
part_of_ds = processed_ds.take(20)

# %%

gpt2_lm.include_preprocessing = False

num_epochs = 1

lr = tf.keras.optimizers.schedules.PolynomialDecay(
    5e-5,
    decay_steps=part_of_ds.cardinality() * num_epochs,
    end_learning_rate=0.0,
)
loss = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)
gpt2_lm.compile(
    optimizer=keras.optimizers.experimental.Adam(lr),
    loss=loss,
    weighted_metrics=["accuracy"])

gpt2_lm.fit(part_of_ds, epochs=num_epochs)

# %%

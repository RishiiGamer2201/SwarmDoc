import os
import glob
import librosa
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import onnx

"""
SwarmDoc - Custom Cough Classifier Training Script for Android ONNX Mobile Use

This script trains a lightweight Multi-Layer Perceptron (MLP) on acoustic features 
(MFCCs, Spectral Centroid, Zero-Crossing Rate) extracted from your COVID-19/Kaggle cough dataset.
It then exports to an ONNX format optimized for SwarmDoc's `OnnxTriageModel` pipeline.

Prerequisites:
  pip install torch onnx librosa numpy
  Dataset: Kaggle Covid-19 Cough Audio dataset containing folders like 'covid', 'healthy', 'bronchitis'

Usage:
  1. Organize your dataset geographically:
     dataset/
       covid_cough/
         1.wav
         2.wav
       healthy_cough/
         3.wav
       wet_cough/
         4.wav
  
  2. Map these to logical labels (e.g., 0=no_cough, 1=dry_cough, 2=wet_cough/covid)
  3. Run this script: python train_cough_mlp.py
"""

# ==========================================
# 1. Feature Extraction Pipeline
# ==========================================
def extract_features(file_path):
    try:
        # Load audio at 16kHz (Standard for mobile inference)
        y, sr = librosa.load(file_path, sr=16000, duration=3.0) 
        
        # 1. Mel-frequency cepstral coefficients (13 coefficients)
        mfccs = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13)
        mfccs_mean = np.mean(mfccs.T, axis=0)
        
        # 2. Spectral Centroid
        centroid = librosa.feature.spectral_centroid(y=y, sr=sr)
        centroid_mean = np.mean(centroid.T, axis=0)
        
        # 3. Spectral Rolloff
        rolloff = librosa.feature.spectral_rolloff(y=y, sr=sr)
        rolloff_mean = np.mean(rolloff.T, axis=0)
        
        # 4. Zero-Crossing Rate
        zcr = librosa.feature.zero_crossing_rate(y)
        zcr_mean = np.mean(zcr.T, axis=0)

        # Concatenate features into a single vector (Length = 13 + 1 + 1 + 1 = 16)
        feature_vector = np.concatenate((mfccs_mean, centroid_mean, rolloff_mean, zcr_mean))
        return feature_vector
        
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return None

# ==========================================
# 2. Dataset Definition
# ==========================================
class CoughDataset(Dataset):
    def __init__(self, data_dir):
        self.features = []
        self.labels = []
        
        # Define your mapping here
        class_mapping = {
            "healthy_cough": 0, # dry/healthy cough
            "wet_cough": 1,     # bronchitis / wet
            "covid_cough": 2    # specific pathology
        }
        
        print("Extracting features (this may take a while)...")
        for class_name, label in class_mapping.items():
            folder_path = os.path.join(data_dir, class_name)
            if not os.path.exists(folder_path):
                print(f"Skipping {folder_path} - not found")
                continue
                
            for file_path in glob.glob(os.path.join(folder_path, '*.wav')):
                feats = extract_features(file_path)
                if feats is not None:
                    self.features.append(feats)
                    self.labels.append(label)
                    
        self.features = np.array(self.features, dtype=np.float32)
        self.labels = np.array(self.labels, dtype=np.longlong)
        
        # Normalize features
        if len(self.features) > 0:
            self.mean = np.mean(self.features, axis=0)
            self.std = np.std(self.features, axis=0)
            # Add eps to prevent div-by-zero
            self.features = (self.features - self.mean) / (self.std + 1e-8)
            print(f"Dataset loaded: {len(self.labels)} samples. Input feature size: {self.features.shape[1]}")

    def __len__(self):
        return len(self.labels)

    def __getitem__(self, idx):
        return self.features[idx], self.labels[idx]

# ==========================================
# 3. Model Definition (Lightweight Mobile MLP)
# ==========================================
class MobileCoughMLP(nn.Module):
    def __init__(self, input_size, num_classes):
        super(MobileCoughMLP, self).__init__()
        # Tiny architecture for snappy mobile execution (<100KB)
        self.network = nn.Sequential(
            nn.Linear(input_size, 32),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(32, 16),
            nn.ReLU(),
            nn.Linear(16, num_classes),
            # Softmax is often done outside or inside, we output raw logits here
            # Mobile app ONNX pipeline will apply Softmax
            nn.Softmax(dim=1) 
        )

    def forward(self, x):
        return self.network(x)

# ==========================================
# 4. Training and Export
# ==========================================
def train_and_export():
    data_dir = "dataset" # Change this to your Kaggle dataset folder
    
    # Check if dataset exists
    if not os.path.exists(data_dir):
        print(f"Please create the '{data_dir}' directory and place your Kaggle wav files inside subfolders (e.g. covid_cough, healthy_cough).")
        return
        
    dataset = CoughDataset(data_dir)
    if len(dataset) == 0:
        print("No valid audio files found. Exiting.")
        return
        
    dataloader = DataLoader(dataset, batch_size=32, shuffle=True)
    
    input_size = 16 # 13 MFCC + Centroid + Rolloff + ZCR
    num_classes = 3 # Healthy vs Wet vs Covid
    
    model = MobileCoughMLP(input_size, num_classes)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001)
    
    print("Training model...")
    epochs = 50
    for epoch in range(epochs):
        total_loss = 0
        for batch_features, batch_labels in dataloader:
            optimizer.zero_grad()
            outputs = model(batch_features)
            loss = criterion(outputs, batch_labels)
            loss.backward()
            optimizer.step()
            total_loss += loss.item()
            
        if (epoch+1) % 10 == 0:
            print(f"Epoch {epoch+1}/{epochs}, Loss: {total_loss/len(dataloader):.4f}")
            
    print("Training complete!")
    
    # Export normalization constants for Android codebase (Very Important)
    print("\n--- Normalization Constants to copy to Android ---")
    print(f"val FEATURE_MEAN = floatArrayOf({', '.join([f'{x}f' for x in dataset.mean])})")
    print(f"val FEATURE_STD = floatArrayOf({', '.join([f'{x}f' for x in dataset.std + 1e-8])})")
    print("---------------------------------------------------\n")
    
    # Export to ONNX
    onnx_path = "cough_classifier_v1.onnx"
    dummy_input = torch.randn(1, input_size) 
    torch.onnx.export(
        model, 
        dummy_input, 
        onnx_path, 
        export_params=True,
        opset_version=11, 
        do_constant_folding=True, 
        input_names=['input'], 
        output_names=['output'],
        dynamic_axes={'input': {0: 'batch_size'}, 'output': {0: 'batch_size'}}
    )
    print(f"Model exported successfully to {onnx_path}.")
    print("Move this file to your Android app's app/src/main/assets/ folder.")

if __name__ == "__main__":
    train_and_export()

// Copyright 2021 The Brave Authors. All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

import SwiftUI
import AVFoundation
import Shared
import SnapKit

struct AddressQRCodeScannerView: View {
  @Binding var address: String
  @State private var isErrorPresented: Bool = false
  @State private var permissionDenied: Bool = false
  @Environment(\.presentationMode) @Binding private var presentationMode
  
  var body: some View {
    NavigationView {
      #if targetEnvironment(simulator)
      ZStack {
        Color.black.ignoresSafeArea()
        Button(action: {
          address = "0xaa32"
          presentationMode.dismiss()
        }) {
          Text("Click here to simulate scan")
            .foregroundColor(.white)
        }
      }
      .navigationBarTitleDisplayMode(.inline)
      .toolbar {
        ToolbarItemGroup(placement: .cancellationAction) {
          Button(action: {
            presentationMode.dismiss()
          }) {
            Text(Strings.CancelString)
              .foregroundColor(Color(.braveOrange))
          }
        }
      }
      .ignoresSafeArea()
      #else
      _AddressQRCodeScannerView(address: $address,
                                isErrorPresented: $isErrorPresented,
                                isPermissionDenied: $permissionDenied,
                                dismiss: { presentationMode.dismiss() }
      )
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
          ToolbarItemGroup(placement: .cancellationAction) {
            Button(action: {
              presentationMode.dismiss()
            }) {
              Text(Strings.CancelString)
                .foregroundColor(Color(.braveOrange))
            }
          }
        }
        .ignoresSafeArea()
        .background(
          Color.clear
            .alert(isPresented: $isErrorPresented) {
              Alert(
                title: Text(""),
                message: Text(Strings.scanQRCodeInvalidDataErrorMessage),
                dismissButton: .default(Text(Strings.scanQRCodeErrorOKButton), action: {
                  presentationMode.dismiss()
                })
              )
            }
        )
        .background(
          Color.clear
            .alert(isPresented: $permissionDenied) {
              Alert(
                title: Text(""),
                message: Text(Strings.scanQRCodePermissionErrorMessage),
                dismissButton: .default(Text(Strings.openPhoneSettingsActionTitle), action: {
                  UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!)
                })
              )
            }
        )
      #endif
    }
  }
}

private struct _AddressQRCodeScannerView: UIViewControllerRepresentable {
  typealias UIViewControllerType = AddressQRCodeScannerViewController
  @Binding var address: String
  @Binding var isErrorPresented: Bool
  @Binding var isPermissionDenied: Bool
  var dismiss: () -> Void
  
  func makeUIViewController(context: Context) -> UIViewControllerType {
    AddressQRCodeScannerViewController(address: _address,
                                isErrorPresented: _isErrorPresented,
                                isPermissionDenied: _isPermissionDenied,
                                dismiss: self.dismiss
    )
  }
  
  func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
  }
}

private class AddressQRCodeScannerViewController: UIViewController {
  @Binding private var address: String
  @Binding private var isErrorPresented: Bool
  @Binding private var isPermissionDenied: Bool
  
  private let captureDevice = AVCaptureDevice.default(for: .video)
  private let captureSession = AVCaptureSession().then {
    $0.sessionPreset = .high
  }
  
  private var previewLayer: AVCaptureVideoPreviewLayer?
  private var isFinishScanning = false
  
  var dismiss: () -> Void
  
  init(
    address: Binding<String>,
    isErrorPresented: Binding<Bool>,
    isPermissionDenied: Binding<Bool>,
    dismiss: @escaping () -> Void
  ) {
    self._address = address
    self._isErrorPresented = isErrorPresented
    self._isPermissionDenied = isPermissionDenied
    self.dismiss = dismiss
    super.init(nibName: nil, bundle: nil)
  }
  
  @available(*, unavailable)
  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }
  
  override func viewDidLoad() {
    super.viewDidLoad()

    view.backgroundColor = .black

    let getAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: AVMediaType.video)
    if getAuthorizationStatus != .denied {
      setupCamera()
    } else {
      isPermissionDenied = true
    }

    let imageView = UIImageView().then {
      $0.image = UIImage(named: "camera-overlay")
      $0.contentMode = .center
      $0.translatesAutoresizingMaskIntoConstraints = false
      $0.isAccessibilityElement = false
    }
    view.addSubview(imageView)
    imageView.snp.makeConstraints {
      $0.center.equalToSuperview()
      $0.width.equalTo(200)
      $0.height.equalTo(200)
    }
  }

  override func viewDidLayoutSubviews() {
    super.viewDidLayoutSubviews()
    
    previewLayer?.frame = view.layer.bounds
  }

  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)

    resetCamera()
    
    if !captureSession.isRunning {
      captureSession.startRunning()
    }
  }

  override func viewWillDisappear(_ animated: Bool) {
    super.viewWillDisappear(animated)

    captureSession.stopRunning()
  }

  private func setupCamera() {
    guard let captureDevice = captureDevice else {
      isErrorPresented = true
      return
    }

    guard let videoInput = try? AVCaptureDeviceInput(device: captureDevice) else {
      isErrorPresented = true
      return
    }
    captureSession.addInput(videoInput)

    let metadataOutput = AVCaptureMetadataOutput()
    if captureSession.canAddOutput(metadataOutput) {
      captureSession.addOutput(metadataOutput)
      metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
      metadataOutput.metadataObjectTypes = [AVMetadataObject.ObjectType.qr]
    } else {
      isErrorPresented = true
      return
    }

    let videoPreviewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
    videoPreviewLayer.videoGravity = AVLayerVideoGravity.resizeAspectFill
    videoPreviewLayer.frame = view.bounds
    view.layer.addSublayer(videoPreviewLayer)
    previewLayer = videoPreviewLayer
    captureSession.startRunning()
  }
}

extension AddressQRCodeScannerViewController: AVCaptureMetadataOutputObjectsDelegate {
  func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
    if let stringValue = (metadataObjects.first as? AVMetadataMachineReadableCodeObject)?.stringValue, stringValue.isETHAddress {
      address = stringValue
      dismiss()
    } else {
      isErrorPresented = true
    }
    isFinishScanning = true
  }
  
  func resetCamera() {
    isFinishScanning = false
  }
}

/* Copyright (c) 2022 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef BRAVE_COMPONENTS_BRAVE_FEDERATED_COMMUNICATION_ADAPTER_H_
#define BRAVE_COMPONENTS_BRAVE_FEDERATED_COMMUNICATION_ADAPTER_H_

#include <memory>
#include <string>

#include "base/callback.h"
#include "base/memory/scoped_refptr.h"
#include "brave/components/brave_federated/learning_service.h"
#include "brave/components/brave_federated/task/typing.h"
#include "services/network/public/cpp/simple_url_loader.h"

namespace network {
class SharedURLLoaderFactory;
class SimpleURLLoader;
}  // namespace network

namespace brave_federated {

class TaskResultResponse;

// This is meant to be an adapter class with a set interface, but since
// we only have one possible adaptations (flower) this is also the flower
// specific implementation
class CommunicationAdapter {
 public:
  explicit CommunicationAdapter(
      scoped_refptr<network::SharedURLLoaderFactory> url_loader_factory);
  ~CommunicationAdapter();

  using PostResultCallback = base::OnceCallback<void(TaskResultResponse)>;
  using GetTaskCallback = base::OnceCallback<void(TaskList)>;

  void GetTasks(GetTaskCallback callback);
  void OnGetTasksHeader(scoped_refptr<net::HttpResponseHeaders> headers);
  void OnGetTasks(GetTaskCallback callback,
                  const std::unique_ptr<std::string> response_body);
  void PostTaskResult(TaskResult result, PostResultCallback callback);
  void OnPostTaskResult(PostResultCallback callback,
                        const std::unique_ptr<std::string> response_body);

 private:
  scoped_refptr<network::SharedURLLoaderFactory> url_loader_factory_;
  std::unique_ptr<network::SimpleURLLoader> url_loader_;
};

}  // namespace brave_federated

#endif  // BRAVE_COMPONENTS_BRAVE_FEDERATED_COMMUNICATION_ADAPTER_H_

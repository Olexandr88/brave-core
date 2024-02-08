#include "brave/ios/browser/api/ui/webui/wallet/blockchain_images_source.h"

#include "base/files/file_util.h"
#include "base/functional/bind.h"
#include "base/memory/ref_counted_memory.h"
#include "base/task/thread_pool.h"
#include "brave/components/brave_wallet/browser/brave_wallet_constants.h"
#include "brave/components/brave_wallet/browser/wallet_data_files_installer.h"
#include "ios/web/public/thread/web_task_traits.h"
#include "ios/web/public/thread/web_thread.h"

namespace brave_wallet {

namespace {

absl::optional<std::string> ReadFileToString(const base::FilePath& path) {
  std::string contents;
  if (!base::ReadFileToString(path, &contents)) {
    return absl::optional<std::string>();
  }
  return contents;
}

}  // namespace

BlockchainImagesSource::BlockchainImagesSource(const base::FilePath& base_path)
    : base_path_(base_path), weak_factory_(this) {}

BlockchainImagesSource::~BlockchainImagesSource() = default;

std::string BlockchainImagesSource::GetSource() const {
  return kImageSourceHost;
}

void BlockchainImagesSource::StartDataRequest(const std::string& path, GotDataCallback callback) {
  DCHECK_CURRENTLY_ON(web::WebThread::UI);

  //const std::string path = web::URLDataSourceIOS::URLToRequestPath(url);

  absl::optional<base::Version> version =
      brave_wallet::GetLastInstalledWalletVersion();
  if (!version) {
    scoped_refptr<base::RefCountedMemory> bytes;
    std::move(callback).Run(std::move(bytes));
    return;
  }

  base::FilePath images_path(base_path_);
  images_path = images_path.AppendASCII(version->GetString());
  images_path = images_path.AppendASCII("images");

  base::ThreadPool::PostTaskAndReplyWithResult(
      FROM_HERE, {base::MayBlock()},
      base::BindOnce(&ReadFileToString, images_path.AppendASCII(path)),
      base::BindOnce(&BlockchainImagesSource::OnGotImageFile,
                     weak_factory_.GetWeakPtr(), std::move(callback)));
}

void BlockchainImagesSource::OnGotImageFile(GotDataCallback callback,
                                            absl::optional<std::string> input) {
  scoped_refptr<base::RefCountedMemory> bytes;
  if (!input) {
    std::move(callback).Run(std::move(bytes));
    return;
  }

  bytes = new base::RefCountedBytes(
      reinterpret_cast<const unsigned char*>(input->c_str()), input->length());
  std::move(callback).Run(std::move(bytes));
}

std::string BlockchainImagesSource::GetMimeType(const std::string& path) const {
  if (base::EndsWith(path, ".png", base::CompareCase::INSENSITIVE_ASCII)) {
    return "image/png";
  }
  if (base::EndsWith(path, ".gif", base::CompareCase::INSENSITIVE_ASCII)) {
    return "image/gif";
  }
  if (base::EndsWith(path, ".jpg", base::CompareCase::INSENSITIVE_ASCII)) {
    return "image/jpg";
  }
  return "image/svg+xml";
}

bool BlockchainImagesSource::AllowCaching() const {
  return true;
}

}  // namespace brave_wallet

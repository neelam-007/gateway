require 'soap/rpc/standaloneServer'
require 'soap/attachment'

class EchoService
  def echoOne(file)
    return file
  end
  
  def echoDir(files)
    return files
  end
end

soapServer = SOAP::RPC::StandaloneServer.new('EchoServer', 'urn:EchoAttachmentsService', '0.0.0.0', 7000)
soapServer.add_servant(EchoService.new)
trap(:INT) do
  soapServer.shutdown
end
soapServer.start


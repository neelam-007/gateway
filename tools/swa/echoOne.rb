require 'soap/rpc/driver'
require 'soap/attachment'

attachment = ARGV.shift || __FILE__

receiveDir = 'received'

#server = 'http://localhost:7700/'
server = 'http://192.168.1.162:8080/axis/services/urn:EchoAttachmentsService'

driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')
# driver.wiredump_dev = STDERR
driver.add_method('echoOne', 'file')

Dir.mkdir(receiveDir) unless File.directory?(receiveDir)

Dir.glob(attachment) do |att|
  File.new("#{receiveDir}/#{att}", "w+") << driver.echoOne(SOAP::Attachment.new(File.open(att))) if File.file?(att)
end



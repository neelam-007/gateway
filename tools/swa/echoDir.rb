require 'soap/rpc/driver'
require 'soap/attachment'

attachment = ARGV.shift || __FILE__

receiveDir = 'received'

#server = 'http://localhost:8080/ssg/soap'
server = 'http://localhost:7700/'
#server = 'http://localhost:7000/'
#server = 'http://192.168.1.162:8080/axis/services/urn:EchoAttachmentsService'

driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')
driver.wiredump_dev = STDERR
driver.add_method('echoDir', 'source')

Dir.mkdir(receiveDir) unless File.directory?(receiveDir)

attRequest = []
Dir.glob(attachment) { |att| attRequest << SOAP::Attachment.new(File.open(att)) if File.file?(att)  }

driver.echoDir(attRequest).each do |x|
    File.new("#{receiveDir}/#{x.contentid}", "w+") << x
end



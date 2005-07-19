require 'net/http'
require 'net/https'
require 'openssl'
require 'optparse'
require 'ostruct'
require 'pp'
  

# The program posts the XML message to the target url, It supports
# https (SSL) including the SSL client certificate authentication. 
# 
# Example: 
#   httppost.rb -p message.xml [-a soap_action] [http://ssg.acme.org/ssg/soap]
#
# The program supports SSL client authentication if the 'https' scheme is used.
# SSL private key and the certificate are expected in the files
# HttpPost.options.ssl.pkey and the HttpPost.options.ssl.cert (PEM format).
#
# httppost.rb -h for help

#The default url endpoint. Override with the command line argument
$default_url = "http://localhost:8080/ssg/soap" 

# Turn on 1.2 mode (Ruby 1.8)
Net::HTTP.version_1_2

class HttpPost
    def initialize(opts, &block)
        @options = opts.dup
        instance_eval(&block) unless block.nil?
    end
    
    def send(content)
        h = Net::HTTP.new(@options.uri.host, @options.uri.port)
        h.use_ssl = true if @options.uri.scheme == "https" # enable SSL/TLS
        if h.use_ssl
            h.key = OpenSSL::PKey::DSA.new(File.read(@options.ssl_pkey_file)) if FileTest.exist?(@options.ssl_pkey_file)
            h.cert = cert = OpenSSL::X509::Certificate.new(File.read(@options.ssl_cert_file)) if FileTest.exist?(@options.ssl_cert_file)
            
            puts "Certificate Subject : #{cert !=nil ? cert.subject : ''}" if @options.verbose
        end
        
        puts "SOAPAction : '#{@options.soapaction}'" if @options.verbose
        puts content if @options.verbose
        
        ret = ''
        resp = nil
        
        h.start() {|h|
            resp = h.post(@options.uri.path, content, @options.headers) do |str|
              ret << str
            end
        }
        case resp
          when Net::HTTPSuccess 
              resp.each_header() { |key, value|
                puts "#{key}, #{value}" if @options.verbose
              } if @options.verbose
          when Net::HTTPRedirection
              @options.uri = URI.parse(resp['location'])
              puts "HTTP Redirection to : '#{@options.uri}'" if @options.verbose
              send(content)
          end
        ret
    end
    
    #
    # The default options
    #
    def HttpPost.default_options()
        opts = OpenStruct.new
        opts.debugmode = false
        opts.soapaction = ""
        opts.outputfile = nil
        opts.postfile = nil
        opts.verbose = false
        #
        #The SSL private key and the certificate files (PEM format)
        #
        opts.ssl_pkey_file = "ssl.pkey"
        opts.ssl_cert_file = "ssl.cert"
        opts
    end
end



if $0 == __FILE__
    #default options
    options = HttpPost.default_options()
    
    ARGV.options do |opts|
        opts.banner = "Usage: ruby #{$0} [OPTIONS] ENDPOINT"
    
        opts.on("-h", "--help", "show this message") {
            puts opts
            exit
        }
        opts.on("-v", "--verbose", TrueClass, "run verbosly") {
            |options.verbose|
        }
        
        opts.on("-p", "--postfile FILE", String, "The file containing the SOAP message (mandatory)"){
            |options.postfile|
        }
        
        opts.on("-a", "--soapaction SOAPAction", String, "The SOAP Action HTTP header, or \"\" if not specified"){
            |options.soapaction|
        }
        opts.parse!
    end || exit
    
    if !options.postfile
        puts ARGV.options 
        exit 
    end
    
    options.uri = if ARGV[0] 
            URI::parse(ARGV[0]) 
          else 
            URI.parse($default_url) 
          end
          
    headers = {'Content-Type' => 'text/xml', 
               'SOAPAction' => options.soapaction }
    options.headers = headers
    
    HttpPost.new(options) {
        puts send(IO.read(@options.postfile))
    }
end
               

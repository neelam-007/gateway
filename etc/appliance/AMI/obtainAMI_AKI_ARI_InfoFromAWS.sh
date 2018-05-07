REGION=$1

if [ -z "${REGION}" ] ; then
  echo "usage: $0 <REGION>"
  exit 1;
fi 

echo "32 BIT for ${REGION}"
echo "----------------------------------------------------------------------------"
ec2-describe-images -o amazon --region $REGION | grep "2.6.18-xenU-ec2-v1.5-i686" 
echo "----------------------------------------------------------------------------"
echo ""
echo ""

echo "----------------------------------------------------------------------------"
echo "64 BIT for ${REGION}"
ec2-describe-images -o amazon --region $REGION | grep "2.6.18-xenU-ec2-v1.5-x86_64"
echo "----------------------------------------------------------------------------"
echo ""
echo ""

echo "----------------------------------------------------------------------------"
echo "Amazon AMI used to unpack tar ball in ${REGION}"
ec2-describe-images -o amazon -F "name=amzn-ami-pv-2012.09.0.i386-ebs" -H --region $REGION
echo "----------------------------------------------------------------------------"
echo ""
echo ""

echo "----------------------------------------------------------------------------"
echo "SSG Security group in ${REGION}"
ec2-describe-group -H -F "group-name=*SSG*" --region $REGION
echo "----------------------------------------------------------------------------"
echo ""
echo ""
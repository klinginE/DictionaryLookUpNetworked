#include <iostream>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <iomanip>

using namespace std;

class datagramObject {

        public:
            int ack = 0;
            int hlen = 0;
            int len = 0;
            int msgNum = 0;
            string hash = "";
            string msg = "";

            datagramObject();

};
datagramObject::datagramObject() {
}

class lookUpClient {

    public:
        enum MSG_TYPE {WELCOME=0x00, CONFIRM=0x01, READY=0x02, NORMAL=0x03, TERMINATE=0x04};
        const static int BLOCK_SIZE = 1024;
        

    private:
        int ack = 0;
        MSG_TYPE msgNum = WELCOME;

};

int main(int argc, char **argv) {

    int fd = -1;
    int port = 4444;
    std::string to("blitz.cs.niu.edu");
    if ((fd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {

        perror("Cannot create socket");
        return 1;

    }

    struct sockaddr_in myaddr;
    memset((char *)&myaddr, 0, sizeof(myaddr));
    myaddr.sin_family = AF_INET;
    myaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    myaddr.sin_port = htons(port);
    
    if (bind(fd, (struct sockaddr*)&myaddr, sizeof(myaddr)) < 0) {
        perror("Cannot bind socket");
        return 1;
    }

    string s("hi");
    datagramObject *dataObject = new datagramObject();
    inet_pton(AF_INET, "131.159.145.90", &myaddr.sin_addr.s_addr);
    short bytes[lookUpClient::BLOCK_SIZE];
    bytes[0] = 1;
    sendto(fd, bytes, lookUpClient::BLOCK_SIZE, 0, (struct sockaddr *)&myaddr, sizeof(myaddr));

   return 0;

}
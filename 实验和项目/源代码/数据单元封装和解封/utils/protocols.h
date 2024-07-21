#ifndef COMPUTER_NETWORK_PROJECT_STRUCT_DEFINE_H
#define COMPUTER_NETWORK_PROJECT_STRUCT_DEFINE_H

#define Ethernet_Frame_Length 18
#define Ipv4_Frame_Length 20
#define UDP_Frame_Length 8
#define Application_Frame_Length 40
 
// 数据链路层————以太网帧结构IEEE 802.3
typedef struct
{
    unsigned char dst_mac[6];
    unsigned char src_mac[6];
    unsigned char type[2];
    unsigned int crc;
} ethernet_frame;

// 网络层————IP数据报
typedef struct
{
    unsigned char version;       // 4 bits
    unsigned char header_length; // 4 bits
    unsigned char type_of_service;
    unsigned short total_length;
    unsigned short identification;
    unsigned char flags;           // 3 bits
    unsigned char fragment_offset; // 13 bits
    unsigned char time_to_live;
    unsigned char protocol;
    unsigned short header_checksum;
    unsigned char src_ip[4];
    unsigned char dst_ip[4];
} ipv4_frame;

// 传输层————UDP
typedef struct
{
    unsigned short src_port;
    unsigned short dst_port;
    unsigned short length;
    unsigned short checksum;
} udp_frame;

// 应用层
typedef struct
{
	unsigned short version; // 2 bits
	unsigned short request; // 2 bits
	unsigned short encode; // 2 bits
	unsigned short state; // 2 bits
	unsigned char *sender; // ?
	unsigned short slen; // 2 bits
	unsigned char time[24]; // 24 bits
	unsigned char *receiver; // ?
	unsigned short rlen; // 2 bits 
    unsigned char *msg;        // ?
    unsigned short mlen; // 2 bits
    unsigned short length; // 2 bits
} application_frame;

#endif // COMPUTER_NETWORK_PROJECT_STRUCT_DEFINE_H

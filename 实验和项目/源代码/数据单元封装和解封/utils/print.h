#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "protocols.h"
#include "crc.h"

#ifndef COMPUTER_NETWORK_PROJECT_PRINT_UTILS_H
#define COMPUTER_NETWORK_PROJECT_PRINT_UTILS_H

void printb(unsigned char*buffer,int len)
{
    printf("----------------- 负载数据段内容(%d bytes) -----------------", len);
    for (int j = len - 1; j >= 0; j--)
    {
        if ((len - 1 - j) % 16 == 0)
        {
            printf("\n");
        }
        printf(" %02x |", buffer[j]);
    }
    printf("\n\n");	
}
void data_link_layer_print(ethernet_frame *data_link)
{
    printf("Data Link Layer:\n");
    printf(" - Source MAC: %02x:%02x:%02x:%02x:%02x:%02x\n", data_link->src_mac[0], data_link->src_mac[1], data_link->src_mac[2], data_link->src_mac[3], data_link->src_mac[4], data_link->src_mac[5]);
    printf(" - Destination MAC: %02x:%02x:%02x:%02x:%02x:%02x\n", data_link->dst_mac[0], data_link->dst_mac[1], data_link->dst_mac[2], data_link->dst_mac[3], data_link->dst_mac[4], data_link->dst_mac[5]);
    printf(" - Type: %02x%02x\n", data_link->type[0], data_link->type[1]);
    printf(" - CRC: %u\n\n", data_link->crc);
}

void ip_print(ipv4_frame *ip)
{
    printf("IP Layer:\n");
    printf(" - Version: %d\n", ip->version);
    printf(" - Header Length: %d\n", ip->header_length);
    printf(" - Type of Service: %d\n", ip->type_of_service);
    printf(" - Total Length: %d\n", ip->total_length);
    printf(" - Identification: %d\n", ip->identification);
    printf(" - Flags: %d\n", ip->flags);
    printf(" - Fragment Offset: %d\n", ip->fragment_offset);
    printf(" - Time to Live: %d\n", ip->time_to_live);
    printf(" - Protocol: %d\n", ip->protocol);
    printf(" - Header Checksum: %d\n", ip->header_checksum);
    printf(" - Source IP: %d.%d.%d.%d\n", ip->src_ip[0], ip->src_ip[1], ip->src_ip[2], ip->src_ip[3]);
    printf(" - Destination IP: %d.%d.%d.%d\n\n", ip->dst_ip[0], ip->dst_ip[1], ip->dst_ip[2], ip->dst_ip[3]);
}

void udp_print(udp_frame *udp)
{
    printf("UDP Layer:\n");
    printf(" - Source Port: %d\n", udp->src_port);
    printf(" - Destination Port: %d\n", udp->dst_port);
    printf(" - Length: %d\n", udp->length);
    printf(" - Checksum: %d\n\n", udp->checksum);
}

void application_layer_print(application_frame *app_layer)
{
    printf("Application Layer:\n");
    printf(" - Total Length: %d\n", app_layer->length);
	printf(" - Version: %d\n", app_layer->version);
	printf(" - State: %d\n", app_layer->state);
	printf(" - Request: %s\n", app_layer->request==0?"Chat":(app_layer->request==1?"login":"Unknown"));
	printf(" - Encoding: %s\n", app_layer->encode==0?"UTF-8":(app_layer->encode==1?"GBK":"Unknown"));
	printf("\n");
	
	printf(" - Time: %s\n", app_layer->time);
	
	printf(" - Sender: ");
	for(int i = 0 ; i < app_layer->slen ; i++)
	printf("%c",app_layer->sender[i]);

	printf("\n - Message:\n");
	for(int i = 0 ; i < app_layer->mlen ; i++)
	printf("%c",app_layer->msg[i]);		
    
	printf("\n - Receiver: ");
	for(int i = 0 ; i < app_layer->rlen ; i++)
	printf("%c",app_layer->receiver[i]);	
	printf("\n");
}

#endif // COMPUTER_NETWORK_PROJECT_PRINT_UTILS_H

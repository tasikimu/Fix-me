package main.java.routerwork;

//enum TYPE { CHECK,DISP}


public interface IResponsibility
{
     int CHECKSUM = 1;
     int DISPATCH = 2;
     int ECHOBACK = 3;
     void performAction(Attachment attach, int resp);
}
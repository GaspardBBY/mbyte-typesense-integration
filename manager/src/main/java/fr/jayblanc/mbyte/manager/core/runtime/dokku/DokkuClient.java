/*
 * Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.jayblanc.mbyte.manager.core.runtime.dokku;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * @author Jerome Blanchard
 */
public class DokkuClient {

    private final String host;
    private final int port;
    private final JSch jsch;

    public DokkuClient(String host, int port, JSch jsch) {
        this.host = host;
        this.port = port;
        this.jsch = jsch;
    }

    public int execute(String username, String command, StringBuffer output) throws JSchException, IOException {
        Session session = jsch.getSession(username, host, port);
        session.connect();

        Channel channel = session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);
        channel.setInputStream(null);

        //((ChannelExec)channel).getErrStream(es);
        //((ChannelExec)channel).setOutputStream(os);

        InputStream in = channel.getInputStream();
        channel.connect();
        byte[] tmp = new byte[1024];
        int status = -1;
        while(true){
            while(in.available()>0){
                int i = in.read(tmp, 0, 1024);
                if ( i < 0 ) {
                    break;
                }
                output.append(new String(tmp, 0, i, Charset.defaultCharset()));
            }
            if( channel.isClosed() ) {
                if ( in.available() > 0 ) {
                    continue;
                }
                status = channel.getExitStatus();
                break;
            }
            try{
                Thread.sleep(1000);
            } catch (Exception ee) {
                //
            }
        }
        channel.disconnect();
        session.disconnect();
        return status;
    }

}

package net.h4bbo.echo.plugin.handshake.messages.login;

import net.h4bbo.echo.api.event.types.player.PlayerLoginEvent;
import net.h4bbo.echo.api.game.player.IPlayer;
import net.h4bbo.echo.api.messages.MessageEvent;
import net.h4bbo.echo.api.network.codecs.DataCodec;
import net.h4bbo.echo.api.network.codecs.IClientCodec;
import net.h4bbo.echo.api.services.user.IUserService;
import net.h4bbo.echo.plugin.handshake.HandshakePlugin;
import net.h4bbo.echo.storage.StorageContextFactory;
import net.h4bbo.echo.codecs.PacketCodec;
import net.h4bbo.echo.storage.models.user.UserData;

import java.sql.SQLException;

public class LoginMessageEvent extends MessageEvent<HandshakePlugin> {
    private final IUserService userService;

    public LoginMessageEvent(IUserService userService) {
        this.userService = userService;
    }

    @Override
    public void handle(IPlayer player, IClientCodec msg) {
        String username = msg.get(DataCodec.STRING);
        String password = msg.get(DataCodec.STRING);

        var userOpt = this.userService.getUserAuthenticate(username, password);

        userOpt.ifPresent(value -> player.attr(UserData.DATA_KEY).setIfAbsent(value));

        boolean loginCancelled = userOpt.isEmpty();

        if (!loginCancelled) {
            loginCancelled = this.getEventManager().publish(new PlayerLoginEvent(player));
        }

        if (loginCancelled) {
            PacketCodec.create(33)
                    .append(DataCodec.BYTES, "Login incorrect")
                    .send(player);
            return;
        }

        player.setAuthenticated(true);

        // PacketCodec.create(139)
        //        .append(DataCodec.BYTES, "Hello " + player.attr(UserData.DATA_KEY).get().getName())
        //                .send(player);

        PacketCodec.create(2)
                .send(player);

        PacketCodec.create(3)
                .send(player);
    }

    @Override
    public int getHeaderId() {
        return 4;
    }
}

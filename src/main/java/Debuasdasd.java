
import com.spleefleague.core.utils.Debugger;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.bracket.Participant;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Jonas
 */
public class Debuasdasd implements Debugger {

    @Override
    public void debug() {
        Participant p = SWC.getInstance().getPlayerManager().get("Preloa").getTournamentParticipant();
        System.out.println(p);
    }
}

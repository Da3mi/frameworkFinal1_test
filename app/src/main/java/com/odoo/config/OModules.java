/*
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * 
 */
package com.odoo.config;

import com.odoo.addons.message.MailGroup;
import com.odoo.addons.message.Message;
import com.odoo.addons.partners.Partners;
import com.odoo.orm.OModel;
import com.odoo.support.OModule;
import com.odoo.support.OModulesHelper;

/**
 * The Class ModulesConfig.
 */
public class OModules extends OModulesHelper {

	OModule partners = new OModule(Partners.class).setDefault();
    OModule messages = new OModule(Message.class).setDefault();
    OModule message_groupe = new OModule(MailGroup.class).setDefault();


    // add(new Module("module_idea", "Idea", new Idea(), 0), true);
   /* add(new Module("module_message", "Message", new com.openerp.addons.message.Message()), true);
    add(new Module("module_mail_groups", "Mail Groups", new com.openerp.addons.message.MailGroup()));
    add(new Module("module_note", "Note", new com.openerp.addons.note.Note()));
    add(new Module("module_meeting", "Meeting/Calendar Event",
            new com.openerp.addons.meeting.Meeting()));*/
}

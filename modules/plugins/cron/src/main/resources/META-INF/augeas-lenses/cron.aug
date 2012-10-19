(*
Module: Cron
 Parses /etc/cron.d/*, /etc/crontab

Author: Raphael Pinson <raphink@gmail.com>

About: Reference
 This lens tries to keep as close as possible to `man 5 crontab` where
 possible.

About: License
  This file is licensed under the GPL.

About: Lens Usage
  Sample usage of this lens in augtool

    * Get the identifier of the devices with a "Clone" option:
      > match "/files/etc/X11/xorg.conf/Device[Option = 'Clone']/Identifier"

About: Configuration files
  This lens applies to /etc/cron.d/* and /etc/crontab. See <filter>.
*)

module Cron =
  autoload xfm

(************************************************************************
 * Group:                 USEFUL PRIMITIVES
 *************************************************************************)

(* Group: Generic primitives *)

(* Variable: eol *)
let eol     = Util.eol

(* Variable: indent *)
let indent  = Util.indent

(* Variable: comment *)
let comment = Util.comment

(* Variable: empty *)
let empty   = Util.empty

(* Variable: num *)
let num        = /[0-9\*][0-9\/,-\*]*/

(* Variable: alpha *)
let alpha      = /[A-Za-z]{3}/

(* Variable: alphanum *)
let alphanum   = num | alpha


(* Group: Separators *)

(* Variable: sep_spc *)
let sep_spc = Util.del_ws_spc

(* Variable: sep_eq *)
let sep_eq  = Util.del_str "="



(************************************************************************
 * Group:                       ENTRIES
 *************************************************************************)


(************************************************************************
 * View: shellvar
 *   A shell variable in crontab
 *************************************************************************)

let shellvar =
  let key_re = Shellvars.key_re - "entry" in
  [ key key_re . sep_eq . Shellvars.simple_value . eol ]


(* View: minute *)
let minute     = [ label "minute"       . store num ]

(* View: hour *)
let hour       = [ label "hour"         . store num ]

(* View: dayofmonth *)
let dayofmonth = [ label "dayofmonth" . store num ]

(* View: month *)
let month      = [ label "month"        . store alphanum ]

(* View: dayofweek *)
let dayofweek  = [ label "dayofweek"  . store alphanum ]


(* View: user *)
let user       = [ label "user"         . store Rx.word ]


(************************************************************************
 * View: time
 *   Time in the format "minute hour dayofmonth month dayofweek"
 *************************************************************************)
let time        = [ label "time" .
                  minute . sep_spc . hour  . sep_spc . dayofmonth
                         . sep_spc . month . sep_spc . dayofweek ]

(* Variable: the valid values for schedules *)
let schedule_re = "reboot" | "yearly" | "annually" | "monthly"
                | "weekly" | "daily"  | "midnight" | "hourly"

(************************************************************************
 * View: schedule
 *   Time in the format "@keyword"
 *************************************************************************)
let schedule    = [ label "schedule" . Util.del_str "@"
                   . store schedule_re ]


(************************************************************************
 * View: entry
 *   A crontab entry
 *************************************************************************)

let entry       = [ label "entry" . indent
                   . ( time | schedule )
                   . sep_spc . user
                   . sep_spc . store Rx.space_in . eol ]


(*
 * View: lns
 *   The cron lens
 *)
let lns = ( empty | comment | shellvar | entry )*


(* Variable: filter *)
let filter =
  incl "/etc/cron.d/*" .
  incl "/etc/crontab" .
  Util.stdexcl

let xfm = transform lns filter
